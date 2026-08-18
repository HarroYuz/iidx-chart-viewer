package com.harroyuz.iidxchartviewer

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class BjmClient {
    private val origin = "https://u.bjmania.com"
    private val cookieManager = CookieManager.getInstance()

    suspend fun fetchScores(): BjmSyncResult = withContext(Dispatchers.IO) {
        val user = authMe() ?: throw BjmException("BJM 登录态不可用，请先登录 BJMANIA")
        val body = requestGrpc("/api/WebUI/GetIidxScores")
        val decoded = IidxScoreProto.decodeGrpcWeb(body)
        BjmSyncResult(user = user, scores = decoded.scores, status = decoded.status)
    }

    fun probeAuthMe(): BjmUser? = authMe()

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

class BjmException(message: String) : Exception(message)

private data class HttpResponse(val code: Int, val body: ByteArray)

private object IidxScoreProto {
    data class Decoded(val scores: List<BjmScore>, val status: Int)

    fun decodeGrpcWeb(body: ByteArray): Decoded {
        val messages = mutableListOf<ByteArray>()
        var offset = 0
        while (offset + 5 <= body.size) {
            val flags = body[offset].toInt() and 0xff
            val length = ((body[offset + 1].toInt() and 0xff) shl 24) or
                ((body[offset + 2].toInt() and 0xff) shl 16) or
                ((body[offset + 3].toInt() and 0xff) shl 8) or
                (body[offset + 4].toInt() and 0xff)
            val end = offset + 5 + length
            if (end > body.size) throw BjmException("BJM gRPC-Web 返回数据不完整")
            if (flags and 0x80 == 0) messages += body.copyOfRange(offset + 5, end)
            offset = end
        }
        if (offset != body.size) throw BjmException("BJM gRPC-Web 返回帧格式错误")
        val payload = ByteArrayOutputStream().apply { messages.forEach(::write) }.toByteArray()
        return decodeResponse(payload)
    }

    private fun decodeResponse(bytes: ByteArray): Decoded {
        val reader = ProtoReader(bytes)
        val scores = mutableListOf<BjmScore>()
        var status = 0
        while (!reader.isAtEnd()) {
            val tag = reader.readVarint()
            val field = (tag shr 3).toInt()
            val wireType = (tag and 7).toInt()
            when {
                field == 1 && wireType == 2 -> scores += decodeScore(reader.readBytes())
                field == 2 && wireType == 0 -> status = reader.readVarint().toInt()
                else -> reader.skip(wireType)
            }
        }
        return Decoded(scores, status)
    }

    private fun decodeScore(bytes: ByteArray): BjmScore {
        val reader = ProtoReader(bytes)
        var musicId = 0
        var playStyle = 0
        var noteId = 0
        var clearFlag = 0
        var missCount = 0
        var time = 0L
        var exScore = 0
        var option1 = 0L
        var option2 = 0L
        while (!reader.isAtEnd()) {
            val tag = reader.readVarint()
            val field = (tag shr 3).toInt()
            val wireType = (tag and 7).toInt()
            if (wireType == 0) {
                val value = reader.readVarint()
                when (field) {
                    1 -> musicId = value.toInt()
                    2 -> playStyle = value.toInt()
                    3 -> noteId = value.toInt()
                    4 -> clearFlag = value.toInt()
                    5 -> missCount = value.toInt()
                    6 -> time = value
                    7 -> exScore = value.toInt()
                    10 -> option1 = value
                    11 -> option2 = value
                }
            } else {
                reader.skip(wireType)
            }
        }
        return BjmScore(musicId, playStyle, noteId, clearFlag, missCount, time, exScore, option1, option2)
    }

    private class ProtoReader(private val bytes: ByteArray) {
        private var offset = 0
        fun isAtEnd() = offset >= bytes.size

        fun readVarint(): Long {
            var value = 0L
            var shift = 0
            while (offset < bytes.size) {
                val current = bytes[offset++].toInt() and 0xff
                value = value or ((current and 0x7f).toLong() shl shift)
                if (current and 0x80 == 0) return value
                shift += 7
                if (shift > 63) throw BjmException("BJM protobuf varint 过长")
            }
            throw BjmException("BJM protobuf 数据不完整")
        }

        fun readBytes(): ByteArray {
            val length = readVarint().toInt()
            if (length < 0 || offset + length > bytes.size) throw BjmException("BJM protobuf 消息不完整")
            return bytes.copyOfRange(offset, offset + length).also { offset += length }
        }

        fun skip(wireType: Int) {
            when (wireType) {
                0 -> readVarint()
                1 -> offset += 8
                2 -> offset += readVarint().toInt()
                5 -> offset += 4
                else -> throw BjmException("不支持的 BJM protobuf 类型：$wireType")
            }
            if (offset > bytes.size) throw BjmException("BJM protobuf 字段越界")
        }
    }
}
