package com.harroyuz.iidxchartviewer.data.remote.bjm

import com.harroyuz.iidxchartviewer.domain.model.ChartRadar
import com.harroyuz.iidxchartviewer.domain.model.RadarAxis
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class BjmChartMetadataProtoTest {
    private fun varint(value: Int): ByteArray = ByteArrayOutputStream().apply {
        var remaining = value
        while (remaining > 127) { write((remaining and 127) or 128); remaining = remaining ushr 7 }
        write(remaining)
    }.toByteArray()
    private fun record(vararg fields: Pair<Int, Int>): ByteArray {
        val body = fields.fold(byteArrayOf()) { bytes, (field, value) -> bytes + varint(field * 8) + varint(value) }
        return byteArrayOf(10) + varint(body.size) + body
    }

    @Test fun decodesHundredthsIntoClockwiseAxisOrder() {
        val data = record(1 to 1000, 2 to 3, 3 to 12345, 4 to 20000, 5 to 8000, 6 to 300, 7 to 4000, 8 to 100)
        val radar = BjmChartMetadataProto.decodeRadars(data).getValue("1000:0:3")
        assertEquals(listOf(123.45f, 80f, 200f, 1f, 40f, 3f), radar.values)
        assertEquals(RadarAxis.SCRATCH, radar.dominantAxis)
    }

    @Test fun skipsZeroPlaceholdersAndKeepsDefaultSpBeginnerType() {
        val data = record(1 to 1000, 2 to 5) + record(1 to 1000, 3 to 500)
        assertEquals(setOf("1000:0:0"), BjmChartMetadataProto.decodeRadars(data).keys)
    }

    @Test fun mapsNoteFieldsWithoutInventingDpBeginner() {
        // First real record from LDJ_notecount_33.bin, plus DP LEGGENDARIA.
        val data = record(1 to 1000, 3 to 99, 4 to 511, 5 to 786, 7 to 99, 8 to 511, 9 to 796, 10 to 1234)
        val notes = BjmChartMetadataProto.decodeNoteCounts(data)
        assertEquals(786, notes["1000:0:3"])
        assertEquals(99, notes["1000:1:1"])
        assertEquals(796, notes["1000:1:3"])
        assertEquals(1234, notes["1000:1:4"])
        assertFalse(notes.containsKey("1000:1:0"))
    }

    @Test fun skipsUnknownWireFields() {
        val data = byteArrayOf(18, 2, 1, 2) + record(1 to 1000, 3 to 99) + byteArrayOf(29, 0, 0, 0, 0)
        assertEquals(99, BjmChartMetadataProto.decodeNoteCounts(data)["1000:0:1"])
    }

    @Test fun rejectsTruncationAndOversizedLengths() {
        listOf(byteArrayOf(10, 5, 8, 1), byteArrayOf(29, 0), byteArrayOf(10, -1, -1, -1, -1, 7), byteArrayOf(0))
            .forEach { bytes -> assertThrows(BjmException::class.java) { BjmChartMetadataProto.decodeRadars(bytes) } }
    }

    @Test fun rejectsRadarOutsideOfficialScale() {
        assertThrows(BjmException::class.java) { BjmChartMetadataProto.decodeRadars(record(1 to 1000, 3 to 20001)) }
    }

    @Test fun tiesUseStableAxisOrderAndZeroIsUnavailable() {
        val tied = ChartRadar(listOf(80f, 80f, 0f, 0f, 0f, 0f))
        assertEquals(RadarAxis.NOTES, tied.dominantAxis)
        assertFalse(ChartRadar(List(6) { 0f }).available)
    }
}
