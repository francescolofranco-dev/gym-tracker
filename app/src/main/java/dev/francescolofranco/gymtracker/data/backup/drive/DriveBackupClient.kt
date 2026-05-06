package dev.francescolofranco.gymtracker.data.backup.drive

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

class DriveApiException(message: String, val httpStatus: Int? = null) : Exception(message)

data class DriveSnapshot(
    val id: String,
    val name: String,
    val createdAt: Instant?,
    val sizeBytes: Long?,
)

/**
 * Raw Drive REST client scoped to the appDataFolder. Pulled out as a thin layer so the rest
 * of the app doesn't see OkHttp / JSON wire details. Caller passes a fresh access token per
 * call (cheap; play-services caches them under the hood).
 */
@Singleton
class DriveBackupClient @Inject constructor(
    private val http: OkHttpClient,
) {

    /** Lists JSON snapshots in the appDataFolder, newest first. */
    fun list(token: String): List<DriveSnapshot> {
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder" +
            "&orderBy=createdTime%20desc" +
            "&fields=files(id,name,createdTime,size)"
        val req = Request.Builder().url(url).get()
            .header("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { res ->
            val body = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw DriveApiException("List failed: $body", res.code)
            val files = JSONObject(body).optJSONArray("files") ?: return emptyList()
            return (0 until files.length()).map { i ->
                val f = files.getJSONObject(i)
                DriveSnapshot(
                    id = f.getString("id"),
                    name = f.getString("name"),
                    createdAt = f.optString("createdTime").takeIf { it.isNotEmpty() }
                        ?.let { runCatching { Instant.parse(it) }.getOrNull() },
                    sizeBytes = f.optString("size").takeIf { it.isNotEmpty() }?.toLongOrNull(),
                )
            }
        }
    }

    /** Uploads [bytes] to the appDataFolder under [name] (multipart upload). Returns the new file id. */
    fun upload(token: String, name: String, bytes: ByteArray, mimeType: String = "application/json"): String {
        val metadata = JSONObject().apply {
            put("name", name)
            put("parents", org.json.JSONArray().apply { put("appDataFolder") })
            put("mimeType", mimeType)
        }.toString()

        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(bytes.toRequestBody(mimeType.toMediaType()))
            .build()

        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
            .post(body)
            .header("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { res ->
            val resBody = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw DriveApiException("Upload failed: $resBody", res.code)
            return JSONObject(resBody).getString("id")
        }
    }

    /** Downloads a file's contents by id. */
    fun download(token: String, fileId: String): ByteArray {
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .get()
            .header("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw DriveApiException("Download failed", res.code)
            return res.body?.bytes() ?: throw DriveApiException("Download body was null")
        }
    }

    fun delete(token: String, fileId: String) {
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId")
            .delete()
            .header("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { res ->
            // 204 = success, 404 = already gone (idempotent), anything else is a real error.
            if (!res.isSuccessful && res.code != 404) {
                throw DriveApiException("Delete failed", res.code)
            }
        }
    }

}
