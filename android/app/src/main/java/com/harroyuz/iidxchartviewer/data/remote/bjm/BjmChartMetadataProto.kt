package com.harroyuz.iidxchartviewer.data.remote.bjm

import com.harroyuz.iidxchartviewer.domain.model.ChartRadar

/** Public LDJ_radar / LDJ_notecount protobufs used by u.bjmania.com. */
internal object BjmChartMetadataProto {
    fun decodeRadars(bytes: ByteArray): Map<String, ChartRadar> = buildMap {
        records(bytes).forEach { fields ->
            val id = fields[1] ?: 0
            val type = fields[2] ?: 0
            if (id > 0 && type in 0..9) {
                val values = listOf(3, 5, 4, 8, 7, 6).map { (fields[it] ?: 0) / 100f }
                if (values.any { it !in 0f..200f }) throw BjmException("BJM 雷达数值超出范围")
                val radar = ChartRadar(values)
                if (radar.available) put("$id:${type / 5}:${type % 5}", radar)
            }
        }
    }

    fun decodeNoteCounts(bytes: ByteArray): Map<String, Int> = buildMap {
        records(bytes).forEach { fields ->
            val id = fields[1] ?: 0
            if (id > 0) for (field in 2..10) {
                // The NOTE database has no DP BEGINNER field.
                val slot = if (field <= 6) field - 2 else field - 1
                fields[field]?.takeIf { it > 0 }?.let { put("$id:${slot / 5}:${slot % 5}", it) }
            }
        }
    }

    private fun records(bytes: ByteArray): List<Map<Int, Int>> = buildList {
        val reader = Reader(bytes)
        while (!reader.atEnd) {
            val tag = reader.varint()
            requireTag(tag)
            if (tag == 10L) {
                val record = Reader(reader.message())
                val fields = mutableMapOf<Int, Int>()
                while (!record.atEnd) {
                    val innerTag = record.varint()
                    requireTag(innerTag)
                    if (innerTag and 7 == 0L) {
                        val value = record.varint()
                        if (value > Int.MAX_VALUE || value < 0) throw BjmException("BJM 谱面字段超出范围")
                        fields[(innerTag ushr 3).toInt()] = value.toInt()
                    } else record.skip((innerTag and 7).toInt())
                }
                add(fields)
            } else reader.skip((tag and 7).toInt())
        }
    }

    private fun requireTag(tag: Long) {
        if (tag <= 0 || tag ushr 3 == 0L) throw BjmException("BJM 谱面字段无效")
    }

    private class Reader(private val bytes: ByteArray) {
        private var offset = 0
        val atEnd get() = offset == bytes.size
        fun varint(): Long {
            var value = 0L
            for (shift in 0..63 step 7) {
                if (offset >= bytes.size) throw BjmException("BJM 谱面数据不完整")
                val byte = bytes[offset++].toInt() and 255
                if (shift == 63 && byte > 1) throw BjmException("BJM 谱面字段过长")
                value = value or ((byte and 127).toLong() shl shift)
                if (byte and 128 == 0) return value
            }
            throw BjmException("BJM 谱面字段过长")
        }
        private fun length(): Int {
            val value = varint()
            if (value < 0 || value > bytes.size - offset) throw BjmException("BJM 谱面字段越界")
            return value.toInt()
        }
        fun message(): ByteArray {
            val size = length()
            return bytes.copyOfRange(offset, offset + size).also { offset += size }
        }
        fun skip(wire: Int) {
            val size = when (wire) {
                0 -> { varint(); 0 }
                1 -> 8
                2 -> length()
                5 -> 4
                else -> throw BjmException("BJM 谱面字段类型不支持")
            }
            if (size > bytes.size - offset) throw BjmException("BJM 谱面字段越界")
            offset += size
        }
    }
}
