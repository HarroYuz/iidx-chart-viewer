package com.harroyuz.iidxchartviewer.data.remote.bjm

import android.webkit.CookieManager
import com.harroyuz.iidxchartviewer.domain.model.BjmMusic
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.BjmUser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BjmClient {
    private val origin = "https://u.bjmania.com"
    private val musicDatabaseOrigin = "https://assets.bjmania.com"
    private val cookieManager = CookieManager.getInstance()

    suspend fun fetchScores(): BjmSyncResult = withContext(Dispatchers.IO) {
        val user = authMe() ?: throw BjmException("BJM 登录态不可用，请先登录 BJMANIA")
        val body = requestGrpc("/api/WebUI/GetIidxScores")
        val decoded = IidxScoreProto.decodeGrpcWeb(body)
        BjmSyncResult(user = user, scores = decoded.scores, status = decoded.status)
    }

    fun probeAuthMe(): BjmUser? = authMe()

    fun clearSession() {
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

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

    private fun authMe(): BjmUser? {
        val response = request("/api/auth/me", "GET", null, "application/json")
        if (response.code !in 200..299) return null
        return runCatching {
            val json = JSONObject(response.body.toString(Charsets.UTF_8))
            BjmUser(
                id = json.optString("id"),
                name = json.optString("name"),
                email = json.optString("email"),
            ).takeIf { it.id.isNotBlank() }
        }.getOrNull()
    }

    private fun requestGrpc(path: String): ByteArray {
        val response = request(
            path = path,
            method = "POST",
            body = ByteArray(0),
            contentType = "application/grpc-web+proto",
        )
        if (response.code !in 200..299) throw BjmException("BJM 成绩请求失败 (${response.code})")
        return response.body
    }

    private fun request(path: String, method: String, body: ByteArray?, contentType: String): HttpResponse {
        val connection = (URL(origin + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            useCaches = false
            doInput = true
            setRequestProperty("Accept", if (contentType == "application/json") "application/json" else "*/*")
            setRequestProperty("Content-Type", contentType)
            val cookieHeader = cookieManager.getCookie(origin).orEmpty()
            setRequestProperty("Cookie", cookieHeader)
            setRequestProperty("Referer", "$origin/")
            if (contentType == "application/grpc-web+proto") {
                setRequestProperty("X-Grpc-Web", "1")
                setRequestProperty("X-User-Agent", "grpc-web-javascript/0.1")
                setRequestProperty("X-Requested-With", "XMLHttpRequest")
                extractCookie(cookieHeader, "XSRF-TOKEN")?.let { setRequestProperty("X-XSRF-TOKEN", it) }
            }
            if (body != null) doOutput = true
        }
        return try {
            body?.let { connection.outputStream.use { output -> output.write(it) } }
            val stream = if (connection.responseCode in 200..399) connection.inputStream else connection.errorStream
            val responseCode = connection.responseCode
            connection.headerFields["Set-Cookie"].orEmpty().forEach { cookie ->
                cookieManager.setCookie(origin, cookie)
            }
            cookieManager.flush()
            HttpResponse(responseCode, stream?.use { it.readBytes() } ?: ByteArray(0))
        } finally {
            connection.disconnect()
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

    private fun extractCookie(header: String, name: String): String? = header
        .split(';')
        .asSequence()
        .map(String::trim)
        .mapNotNull { item ->
            val separator = item.indexOf('=')
            if (separator <= 0 || item.substring(0, separator) != name) null
            else item.substring(separator + 1)
        }
        .firstOrNull()
        ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
}

data class BjmSyncResult(
    val user: BjmUser,
    val scores: List<BjmScore>,
    val status: Int,
)

class BjmException(message: String, cause: Throwable? = null) : Exception(message, cause)

private data class HttpResponse(val code: Int, val body: ByteArray)
