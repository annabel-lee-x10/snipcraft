package dev.a10101100.snipcraft.core.sync

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    data class Credentials(val username: String, val password: String) {
        fun basicHeader(): String {
            val encoded = Base64.encodeToString(
                "$username:$password".toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP,
            )
            return "Basic $encoded"
        }
    }

    sealed class PropFindResult {
        object Found : PropFindResult()
        object NotFound : PropFindResult()
        data class Error(val message: String) : PropFindResult()
    }

    suspend fun propFind(
        url: String,
        credentials: Credentials,
        allowHttp: Boolean = false,
    ): PropFindResult {
        if (!allowHttp && url.startsWith("http://")) {
            return PropFindResult.Error("Refusing plain HTTP URL — enable 'Allow plain HTTP' to override")
        }
        return try {
            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaType()))
                .header("Depth", "0")
                .header("Authorization", credentials.basicHeader())
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.use {
                when (it.code) {
                    207, 200 -> PropFindResult.Found
                    404      -> PropFindResult.NotFound
                    else     -> PropFindResult.Error("PROPFIND failed: HTTP ${it.code}")
                }
            }
        } catch (e: Exception) {
            PropFindResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun get(url: String, credentials: Credentials): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", credentials.basicHeader())
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.use { if (it.isSuccessful) it.body?.string() else null }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun put(url: String, credentials: Credentials, body: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .put(body.toRequestBody("application/json".toMediaType()))
                .header("Authorization", credentials.basicHeader())
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.use { it.code in 200..299 }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun mkCol(url: String, credentials: Credentials): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .method("MKCOL", null)
                .header("Authorization", credentials.basicHeader())
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.use { it.code in 200..299 || it.code == 405 }
        } catch (e: Exception) {
            false
        }
    }
}
