package com.hyundaiapp.core.backend

import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

data class BackendConfig(val url: String, val publishableKey: String, val app: String, val simulated: Boolean) {
    val storageNamespace: String get() {
        val digest = MessageDigest.getInstance("SHA-256").digest((url.trimEnd('/') + ":" + app).toByteArray())
            .take(8).joinToString("") { "%02x".format(it.toInt() and 255) }
        return "supabase-${if (simulated) "demo" else "live"}-$digest"
    }
    init {
        val uri = URI(url)
        require(uri.scheme == "https" && uri.host != null && uri.rawQuery == null && uri.userInfo == null)
        require(publishableKey.startsWith("sb_publishable_"))
        require(app in setOf("CHACHELIN", "CRUISEBREAKER"))
    }
}

data class BackendResponse(val status: Int, val body: String)

fun interface BackendTransport {
    fun post(path: String, body: String, accessToken: String?): BackendResponse
}

/** Errors expose only status/code, never JWTs, request bodies or raw Auth responses. */
class BackendException(val status: Int, val code: String) : Exception("Backend HTTP $status ($code)")

class HttpsBackendTransport(private val config: BackendConfig) : BackendTransport {
    override fun post(path: String, body: String, accessToken: String?): BackendResponse {
        require(path.startsWith("/auth/v1/") || path.startsWith("/rest/v1/rpc/pleos_"))
        val connection = URI(config.url.trimEnd('/') + path).toURL().openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 7_000
            connection.readTimeout = 7_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("apikey", config.publishableKey)
            connection.setRequestProperty("Content-Type", "application/json")
            accessToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            BackendResponse(status, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally { connection.disconnect() }
    }
}

interface BackendStore {
    fun read(): String?
    /** Must complete an atomic, durable write before returning. */
    fun write(value: String)
}
