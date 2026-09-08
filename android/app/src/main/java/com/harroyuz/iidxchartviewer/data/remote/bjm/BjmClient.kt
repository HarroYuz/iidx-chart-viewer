package com.harroyuz.iidxchartviewer.data.remote.bjm

import com.harroyuz.iidxchartviewer.domain.model.BjmUser
import com.harroyuz.iidxchartviewer.domain.model.BjmMusic
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.BuildConfig

import android.content.Context
import android.webkit.WebSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class BjmClient(context: Context) {
    private val origin = "https://u.bjmania.com"
    private val musicDatabaseOrigin = "https://assets.bjmania.com"
    private val sessionManager = BjmSessionManager.getInstance(context)

    init {
        sessionManager.setUserAgent(
            WebSettings.getDefaultUserAgent(context) + " IIDXChartViewer/${BuildConfig.VERSION_NAME}",
        )
    }

    suspend fun fetchScores(): BjmSyncResult = withContext(Dispatchers.IO) {
        val user = authMe()
        val body = requestGrpc("/api/WebUI/GetIidxScores")
        val decoded = IidxScoreProto.decodeGrpcWeb(body)
        BjmSyncResult(user = user, scores = decoded.scores, status = decoded.status)
    }

    /** Read the persistent WebView session when authentication is explicitly requested. */
    fun probeAuthMe(): BjmAuthResult = authMeResult()

    fun clearSession() = sessionManager.clearAllSession()

    suspend fun fetchMusicDatabase(): List<BjmMusic> = withContext(Dispatchers.IO) {
        val version = runCatching {
            val response = requestPublic("$musicDatabaseOrigin/mdb/ver.json")
            val json = JSONObject(response.toString(StandardCharsets.UTF_8))
            val versions = json.optJSONObject("LDJ")
                ?.optJSONObject("mdb")
                ?.keys()
                ?.asSequence()
                ?.mapNotNull { it.toIntOrNull() }
                ?.toList()
                .orEmpty()
            versions.maxOrNull()?.toString()
        }.getOrNull() ?: "33"
        BjmMusicProto.decode(requestPublic("$musicDatabaseOrigin/mdb/LDJ_mdb_$version.bin"))
            .takeIf { it.isNotEmpty() }
            ?: throw BjmException("BJM 音乐数据库为空")
    }

    private fun authMe(): BjmUser {
        val result = authMeResult()
        return result.user ?: throw BjmAuthException(
            result.failure ?: BjmAuthFailure(BjmAuthFailureKind.INVALID_RESPONSE),
        )
    }

    private fun authMeResult(): BjmAuthResult {
        val response = try {
            withBjmAuthRecovery(
                request = sessionManager::probeAuthMeWithWebViewCookies,
                status = { it.statusCode },
                refresh = { sessionManager.refreshWebViewCookiesBlocking() },
            )
        } catch (error: IOException) {
            return BjmAuthResult(null, BjmAuthFailure(BjmAuthFailureKind.NETWORK, causeMessage = error.message), 0, 0, false)
        }

        if (!response.hadCookie) {
            return BjmAuthResult(
                user = null,
                failure = BjmAuthFailure(BjmAuthFailureKind.NO_COOKIE),
                statusCode = response.statusCode,
                cookieLength = response.cookieLength,
                hadCookie = false,
            )
        }
        if (!response.success) {
            return BjmAuthResult(
                user = null,
                failure = BjmAuthFailure.fromHttpStatus(response.statusCode),
                statusCode = response.statusCode,
                cookieLength = response.cookieLength,
                hadCookie = true,
            )
        }

        val user = runCatching {
            val json = JSONObject(response.body)
            BjmUser(
                id = json.optString("id"),
                name = json.optString("name"),
                email = json.optString("email"),
            ).takeIf { it.id.isNotBlank() }
        }.getOrNull()
        return if (user != null) {
            BjmAuthResult(
                user = user,
                failure = null,
                statusCode = response.statusCode,
                cookieLength = response.cookieLength,
                hadCookie = true,
            )
        } else {
            BjmAuthResult(
                user = null,
                failure = BjmAuthFailure(BjmAuthFailureKind.INVALID_RESPONSE, response.statusCode),
                statusCode = response.statusCode,
                cookieLength = response.cookieLength,
                hadCookie = true,
            )
        }
    }

    private fun requestGrpc(path: String): ByteArray {
        val response = request(
            path = path,
            method = "POST",
            body = ByteArray(0),
            contentType = "application/grpc-web+proto",
        )
        if (response.code == 401 || response.code == 403) {
            throw BjmAuthException(BjmAuthFailure.fromHttpStatus(response.code))
        }
        if (response.code !in 200..299) throw BjmException("BJM 成绩请求失败 (${response.code})")
        return response.body
    }

    private fun request(path: String, method: String, body: ByteArray?, contentType: String): HttpResponse {
        return withBjmAuthRecovery(
            request = { executeRequest(path, method, body, contentType) },
            status = { it.code },
            refresh = { sessionManager.refreshWebViewCookiesBlocking() },
        )
    }

    private fun executeRequest(path: String, method: String, body: ByteArray?, contentType: String): HttpResponse {
        sessionManager.syncFromWebViewCookieManager()
        val request = Request.Builder()
            .url(origin + path)
            .header("Accept", if (contentType == "application/json") "application/json" else "*/*")
            .header("Content-Type", contentType)
            .apply {
                if (contentType == "application/grpc-web+proto") {
                    header("X-Grpc-Web", "1")
                    header("X-User-Agent", "grpc-web-javascript/0.1")
                }
                method(
                    method,
                    body?.toRequestBody(contentType.toMediaType()),
                )
            }
            .build()
        return sessionManager.client().newCall(request).execute().use { response ->
            HttpResponse(response.code, response.body?.bytes() ?: ByteArray(0))
        }
    }

    private fun requestPublic(url: String): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            useCaches = false
            doInput = true
            setRequestProperty("Accept", "application/octet-stream, application/json")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..399) connection.inputStream else connection.errorStream
            if (code !in 200..299) throw BjmException("BJM 曲目数据库请求失败 ($code)")
            stream?.use { it.readBytes() } ?: ByteArray(0)
        } finally {
            connection.disconnect()
        }
    }

}

data class BjmSyncResult(
    val user: BjmUser,
    val scores: List<BjmScore>,
    val status: Int,
)

enum class BjmAuthFailureKind {
    NO_COOKIE,
    UNAUTHORIZED,
    FORBIDDEN,
    REDIRECT,
    SERVER_ERROR,
    HTTP_ERROR,
    INVALID_RESPONSE,
    NETWORK,
}

data class BjmAuthFailure(
    val kind: BjmAuthFailureKind,
    val statusCode: Int = 0,
    val causeMessage: String? = null,
) {
    val userMessage: String
        get() = when (kind) {
            BjmAuthFailureKind.NO_COOKIE -> "未检测到 BJM 登录 Cookie，请先登录 BJM"
            BjmAuthFailureKind.UNAUTHORIZED -> "BJM 登录态已失效（HTTP 401），请重新登录"
            BjmAuthFailureKind.FORBIDDEN -> "BJM 拒绝了当前登录态（HTTP 403），请重新登录"
            BjmAuthFailureKind.REDIRECT -> "BJM 登录验证被重定向（HTTP $statusCode），请重新登录"
            BjmAuthFailureKind.SERVER_ERROR -> "BJM 服务器异常（HTTP $statusCode），请稍后重试"
            BjmAuthFailureKind.HTTP_ERROR -> "BJM 登录验证失败（HTTP $statusCode）"
            BjmAuthFailureKind.INVALID_RESPONSE -> "BJM 登录验证响应格式异常，请稍后重试"
            BjmAuthFailureKind.NETWORK -> "无法连接 BJM，请检查网络"
        }

    companion object {
        fun fromHttpStatus(statusCode: Int): BjmAuthFailure = when (statusCode) {
            401 -> BjmAuthFailure(BjmAuthFailureKind.UNAUTHORIZED, statusCode)
            403 -> BjmAuthFailure(BjmAuthFailureKind.FORBIDDEN, statusCode)
            in 300..399 -> BjmAuthFailure(BjmAuthFailureKind.REDIRECT, statusCode)
            in 500..599 -> BjmAuthFailure(BjmAuthFailureKind.SERVER_ERROR, statusCode)
            else -> BjmAuthFailure(BjmAuthFailureKind.HTTP_ERROR, statusCode)
        }
    }
}

data class BjmAuthResult(
    val user: BjmUser?,
    val failure: BjmAuthFailure?,
    val statusCode: Int,
    val cookieLength: Int,
    val hadCookie: Boolean,
)

open class BjmException(message: String, cause: Throwable? = null) : Exception(message, cause)

class BjmAuthException(
    val failure: BjmAuthFailure,
    cause: Throwable? = null,
) : BjmException(failure.userMessage, cause)

private data class HttpResponse(val code: Int, val body: ByteArray)
