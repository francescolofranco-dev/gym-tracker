package dev.francescolofranco.gymtracker.data.backup.drive

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DriveAuthorizationOutcome {
    data class Granted(val accessToken: String) : DriveAuthorizationOutcome
    data class NeedsUserConsent(val pendingIntent: PendingIntent) : DriveAuthorizationOutcome
}

class DriveAuthorizationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Owns the Google Identity Services authorization for Drive's appDataFolder. Authentication is
 * intentionally separate: the app does not need a user profile, only permission to access its
 * private Drive storage. Google Play services caches the grant and returns a short-lived access
 * token silently on later calls, including calls made by the daily backup worker.
 *
 * Runtime requires an Android OAuth client configured with this package name and signing SHA-1.
 */
@Singleton
class DriveAuth @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val authorizationClient by lazy { Identity.getAuthorizationClient(context) }
    private val requestedScopes = listOf(Scope(DRIVE_APPDATA_SCOPE))
    private val authorizationRequest by lazy {
        AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()
    }

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    /**
     * Requests the Drive grant. Previously granted access returns immediately; otherwise the
     * caller must launch the returned consent PendingIntent with StartIntentSenderForResult.
     */
    suspend fun requestAuthorization(): DriveAuthorizationOutcome {
        val result = try {
            authorizationClient.authorize(authorizationRequest).await()
        } catch (e: ApiException) {
            throw authorizationFailure(e)
        }

        if (result.hasResolution()) {
            _connected.value = false
            val pendingIntent = result.pendingIntent
                ?: throw DriveAuthorizationException("Google Drive authorization could not be started.")
            return DriveAuthorizationOutcome.NeedsUserConsent(pendingIntent)
        }

        return grantedOutcome(result)
    }

    /** Completes an interactive authorization launched from the Settings screen. */
    suspend fun handleAuthorizationResult(data: Intent?): DriveAuthorizationOutcome.Granted {
        val intent = data ?: throw DriveAuthorizationException("Google Drive authorization was cancelled.")
        val result = try {
            authorizationClient.getAuthorizationResultFromIntent(intent)
        } catch (e: ApiException) {
            throw authorizationFailure(e)
        }
        return grantedOutcome(result)
    }

    /** Refreshes the observable connection state without ever opening consent UI. */
    suspend fun refreshAuthorization() {
        runCatching { requestAuthorization() }
            .onFailure { Log.w(TAG, "Unable to refresh Drive authorization: ${it.message}") }
    }

    /**
     * Gets a current short-lived bearer token. Google recommends calling authorize() again in
     * later sessions; granted permissions are reused without user interaction. A null result means
     * the grant now needs interactive consent or Google Play services could not refresh it.
     */
    suspend fun freshAccessToken(): String? = try {
        when (val outcome = requestAuthorization()) {
            is DriveAuthorizationOutcome.Granted -> outcome.accessToken
            is DriveAuthorizationOutcome.NeedsUserConsent -> null
        }
    } catch (e: DriveAuthorizationException) {
        Log.w(TAG, "Unable to obtain Drive access token: ${e.message}")
        null
    }

    /** Revokes the app's Drive grant and clears Google Play services' locally cached tokens. */
    suspend fun revokeAccess() {
        val request = RevokeAccessRequest.builder()
            .setScopes(requestedScopes)
            .build()
        try {
            authorizationClient.revokeAccess(request).await()
            _connected.value = false
        } catch (e: ApiException) {
            throw authorizationFailure(e)
        }
    }

    private fun grantedOutcome(result: AuthorizationResult): DriveAuthorizationOutcome.Granted {
        if (DRIVE_APPDATA_SCOPE !in result.grantedScopes) {
            _connected.value = false
            throw DriveAuthorizationException("Google Drive permission was not granted.")
        }
        val token = result.accessToken
        if (token == null) {
            _connected.value = false
            throw DriveAuthorizationException("Google did not return a Drive access token.")
        }
        _connected.value = true
        return DriveAuthorizationOutcome.Granted(token)
    }

    private fun authorizationFailure(error: ApiException): DriveAuthorizationException {
        val message = humanReadableError(error.statusCode)
        Log.w(TAG, "Authorization failed: status=${error.statusCode} ($message) raw=${error.message}")
        return DriveAuthorizationException(message, error)
    }

    private fun humanReadableError(statusCode: Int): String = when (statusCode) {
        CommonStatusCodes.DEVELOPER_ERROR ->
            "Authorization rejected by Google (DEVELOPER_ERROR / 10). Verify that the Android " +
                "OAuth client uses package ${context.packageName} and the signing certificate's SHA-1."
        CommonStatusCodes.INTERNAL_ERROR ->
            "This build is not registered for Google OAuth (status 8). Add an Android OAuth client " +
                "for package ${context.packageName} and this build's SHA-1 certificate fingerprint " +
                "in Google Cloud Console."
        CommonStatusCodes.NETWORK_ERROR ->
            "Network error reaching Google. Check connectivity and retry."
        CommonStatusCodes.SIGN_IN_REQUIRED ->
            "A Google account is required. Add an account to the device and retry."
        CommonStatusCodes.API_NOT_CONNECTED ->
            "Google Play services is not available on this device."
        CommonStatusCodes.CANCELED, 12501 ->
            "Google Drive authorization was cancelled."
        else -> "Google Drive authorization failed (status $statusCode)."
    }

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val TAG = "DriveAuth"
    }
}

// Tiny suspend wrapper around play-services Tasks. Avoids an extra dependency for four calls.
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> if (cont.isActive) cont.resumeWith(Result.success(result)) }
        addOnFailureListener { error -> if (cont.isActive) cont.resumeWith(Result.failure(error)) }
        addOnCanceledListener { cont.cancel() }
    }
}
