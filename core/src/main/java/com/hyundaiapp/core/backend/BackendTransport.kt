package com.hyundaiapp.core.backend

import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

/** [consentVersion]은 사용자가 동의한 약관 판 번호. 서버 `app_installations.consent_version`에 남는다(docs/terms-draft.md) */
data class BackendConfig(
    val url: String,
    val publishableKey: String,
    val app: String,
    val simulated: Boolean,
    val consentVersion: String = "development-v1",
) {
    val storageNamespace: String get() {
        val digest = MessageDigest.getInstance("SHA-256").digest((url.trimEnd('/') + ":" + app).toByteArray())
            .take(8).joinToString("") { "%02x".format(it.toInt() and 255) }
        return "supabase-${if (simulated) "demo" else "live"}-$digest"
    }
    init {
        // 메시지에 키 값은 넣지 않는다(로그에 남음)
        val uri = runCatching { URI(url) }.getOrElse { throw IllegalArgumentException("url is not a valid URI") }
        require(uri.scheme == "https" && uri.host != null && uri.rawQuery == null && uri.userInfo == null) {
            "url must be https://<host>[/path] without query or user info"
        }
        require(publishableKey.startsWith("sb_publishable_")) {
            "publishableKey must start with sb_publishable_ (legacy anon JWT keys are not supported; check for quotes/whitespace)"
        }
        require(app in setOf("CHACHELIN", "CRUISEBREAKER")) { "unknown app $app" }
        require(consentVersion.length in 1..100) { "consentVersion must be 1..100 chars" }
    }

    companion object {
        /**
         * 빌드 설정값(local.properties → BuildConfig)으로 설정을 만든다.
         * - URL이 비어 있으면 서버 없이(Fake) 쓰는 뜻이므로 조용히 null
         * - 값이 있는데 형식이 틀리면(http, 구형 anon JWT, 따옴표·공백 섞임) [onInvalid]에 이유를 알리고 null
         *   → 앱이 켜지자마자 죽는 대신 서버 없이 뜬다. 제출 빌드는 tools/build-release.sh가 같은 형식을 먼저 검사한다
         * 앞뒤 공백·줄바꿈은 지운다(local.properties 끝 공백 때문에 죽지 않게).
         */
        fun fromBuildConfigOrNull(
            url: String,
            publishableKey: String,
            app: String,
            simulated: Boolean,
            consentVersion: String,
            onInvalid: (reason: String) -> Unit = {},
        ): BackendConfig? {
            val trimmedUrl = url.trim()
            if (trimmedUrl.isEmpty()) return null
            return try {
                BackendConfig(trimmedUrl, publishableKey.trim(), app, simulated, consentVersion)
            } catch (e: Exception) {
                onInvalid("backend config rejected, starting without server: ${e.message ?: e.javaClass.simpleName}")
                null
            }
        }
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
