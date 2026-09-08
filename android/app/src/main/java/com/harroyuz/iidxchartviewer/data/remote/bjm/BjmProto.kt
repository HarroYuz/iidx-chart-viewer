package com.harroyuz.iidxchartviewer.data.remote.bjm

import com.harroyuz.iidxchartviewer.domain.model.BjmMusic
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

internal object IidxScoreProto {
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
                2 -> {
                    val length = readVarint().toInt()
                    offset += length
                }
                5 -> offset += 4
                else -> throw BjmException("不支持的 BJM protobuf 类型：$wireType")
            }
            if (offset > bytes.size) throw BjmException("BJM protobuf 字段越界")
        }
    }
}

internal object BjmMusicProto {
    fun decode(bytes: ByteArray): List<BjmMusic> {
        val reader = MusicProtoReader(bytes)
        val result = mutableListOf<BjmMusic>()
        var recordIndex = 0
        while (!reader.isAtEnd()) {
            val tag = reader.readVarint()
            val field = (tag shr 3).toInt()
            val wireType = (tag and 7).toInt()
            if (field == 1 && wireType == 2) {
                try {
                    result += decodeMusic(reader.readBytes())
                } catch (error: BjmException) {
                    throw BjmException("BJM 音乐数据库第 ${recordIndex + 1} 条记录解析失败：${error.message}", error)
                }
                recordIndex++
            } else {
                reader.skip(wireType)
            }
        }
        return result.filter { it.musicId > 0 && it.title.isNotBlank() }
    }

    private fun decodeMusic(bytes: ByteArray): BjmMusic {
        val reader = MusicProtoReader(bytes)
        var musicId = 0
        var title = ""
        var plainTitle = ""
        var genre = ""
        var artist = ""
        var version = 0
        val levels = MutableList(10) { "" }
        while (!reader.isAtEnd()) {
            val tag = reader.readVarint()
            val field = (tag shr 3).toInt()
            val wireType = (tag and 7).toInt()
            try {
                when {
                    field == 1 && wireType == 0 -> musicId = reader.readVarint().toInt()
                    field == 2 && wireType == 2 -> title = reader.readString()
                    field == 3 && wireType == 2 -> plainTitle = reader.readString()
                    field == 4 && wireType == 2 -> genre = reader.readString()
                    field == 5 && wireType == 2 -> artist = reader.readString()
                    field == 12 && wireType == 0 -> version = reader.readVarint().toInt()
                    field in 16..25 && wireType == 2 -> levels[field - 16] = reader.readString()
                    field in 26..35 && wireType == 2 -> reader.readString()
                    else -> reader.skip(wireType)
                }
            } catch (error: BjmException) {
                throw BjmException("音乐字段 field=$field wire=$wireType position=${reader.position()}", error)
            }
        }
        return BjmMusic(musicId, title, plainTitle, genre, artist, version, levels)
    }

    private class MusicProtoReader(private val bytes: ByteArray) {
        private var offset = 0

        fun isAtEnd(): Boolean = offset >= bytes.size

        fun position(): Int = offset

        fun readVarint(): Long {
            var value = 0L
            var shift = 0
            while (offset < bytes.size) {
                val current = bytes[offset++].toInt() and 0xff
                value = value or ((current and 0x7f).toLong() shl shift)
                if (current and 0x80 == 0) return value
                shift += 7
                if (shift > 63) throw BjmException("BJM 音乐数据库字段过长")
            }
            throw BjmException("BJM 音乐数据库数据不完整（位置 $offset/${bytes.size}）")
        }

        fun readBytes(): ByteArray {
            val length = readVarint().toInt()
            if (length < 0 || offset + length > bytes.size) throw BjmException("BJM 音乐数据库消息不完整")
            return bytes.copyOfRange(offset, offset + length).also { offset += length }
        }

        fun readString(): String = readBytes().toString(StandardCharsets.UTF_8)

        fun skip(wireType: Int) {
            when (wireType) {
                0 -> readVarint()
                1 -> offset += 8
                2 -> {
                    val length = readVarint().toInt()
                    offset += length
                }
                5 -> offset += 4
                else -> throw BjmException("不支持的 BJM 音乐数据库字段类型：$wireType")
            }
            if (offset > bytes.size) throw BjmException("BJM 音乐数据库字段越界")
        }
    }
}
