package dev.francescolofranco.gymtracker.data.backup.drive

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the legacy GoogleSignIn flow + OAuth token retrieval. We use the appDataFolder scope,
 * which sandboxes our backups in an app-specific area of the user's Drive without needing the
 * full Drive scope (no scary "view all your files" warning).
 *
 * The Credential Manager rewrite is the long-term path; legacy GoogleSignIn still works fine
 * for getting an account + scope-bounded OAuth tokens, and is enough for backup-only flows.
 *
 * Runtime requires an OAuth client ID configured in Google Cloud Console with this app's
 * package name + signing-cert SHA. Without that, sign-in fails with `DEVELOPER_ERROR` (status
 * code 10) — which the Settings UI surfaces with a hint.
 */
@Singleton
class DriveAuth @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _account = MutableStateFlow(GoogleSignIn.getLastSignedInAccount(context))
    val account: StateFlow<GoogleSignInAccount?> = _account.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val signInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    fun signInIntent(): Intent = signInClient.signInIntent

    /** Call from your ActivityResult callback with the launcher's result data. */
    fun handleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            _account.value = task.getResult(ApiException::class.java)
            _lastError.value = null
        } catch (e: ApiException) {
            val human = humanReadableError(e.statusCode)
            Log.w(TAG, "Sign-in failed: status=${e.statusCode} ($human) raw=${e.message}")
            _lastError.value = human
            _account.value = null
        }
    }

    fun consumeError() {
        _lastError.value = null
    }

    private fun humanReadableError(statusCode: Int): String = when (statusCode) {
        CommonStatusCodes.DEVELOPER_ERROR -> // 10
            "Sign-in rejected by Google (DEVELOPER_ERROR / 10). Usually means the OAuth client " +
                "in Google Cloud Console doesn't match this app's package + signing SHA-1. Verify " +
                "the OAuth client is type Android, package = ${context.packageName}, and the SHA-1 " +
                "of the keystore you're running matches what's registered."
        CommonStatusCodes.NETWORK_ERROR -> // 7
            "Network error reaching Google. Check connectivity and retry."
        CommonStatusCodes.SIGN_IN_REQUIRED -> // 4
            "Sign-in required. Try again."
        CommonStatusCodes.API_NOT_CONNECTED -> // 17
            "Google Play services not available on this device."
        CommonStatusCodes.CANCELED, 12501 -> // 16 / 12501
            "Sign-in cancelled."
        else -> "Sign-in failed (status $statusCode)."
    }

    suspend fun signOut() {
        signInClient.signOut().await()
        _account.value = null
    }

    /**
     * Block-fetches a fresh OAuth bearer token for the appDataFolder scope. Called from a
     * background thread (the worker) — caller is responsible for IO dispatching when relevant.
     * Returns null if no account is signed in.
     */
    suspend fun freshAccessToken(): String? = withContext(Dispatchers.IO) {
        val acct = _account.value ?: GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        val email = acct.email ?: return@withContext null
        runCatching {
            GoogleAuthUtil.getToken(context, Account(email, "com.google"), "oauth2:$DRIVE_APPDATA_SCOPE")
        }.getOrNull()
    }

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val SIGN_IN_REQUEST_CODE = 9001
        private const val TAG = "DriveAuth"
    }
}

// Tiny suspend wrapper around play-services Tasks. Avoids pulling in kotlinx-coroutines-play-services
// just for one signOut call.
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> if (cont.isActive) cont.resumeWith(Result.success(result)) }
        addOnFailureListener { e -> if (cont.isActive) cont.resumeWith(Result.failure(e)) }
        addOnCanceledListener { cont.cancel() }
    }
}
