package com.harroyuz.iidxchartviewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextageClientTest {
    @Test
    fun decodesChargeNoteAcrossMeasures() {
        val source = """
            if(k){
                c1[1]=[[5,0,128,1]];
                c1[2]=[[5,0,30,2]];
                sp[1]="";
                sp[2]="";
            }else{
                sp[1]="";
            }
        """.trimIndent()
        val chart = IidxChart(
            id = "test",
            title = "test",
            mode = "SP",
            difficulty = "H",
            level = 1,
            notes = 1,
            version = "test",
            bpm = "160",
        )

        val parsed = TextageParser.parseChart(chart, source)
        val hold = parsed.notes.firstOrNull { it.lane == 5 }

        assertTrue(parsed.parsed)
        assertEquals(0f, hold?.beat ?: -1f, 0.001f)
        assertEquals(4.9375f, hold?.holdBeats ?: -1f, 0.001f)
    }

    @Test
    fun selectsDpElseBranchWithoutRegexError() {
        val source = """
            if(k){
                sp[1]="01";
            }else{
                sp[1]="01";
                dp[1]="01";
            }
        """.trimIndent()
        val chart = IidxChart(
            id = "test-dp",
            title = "test",
            mode = "DP",
            difficulty = "H",
            level = 1,
            notes = 1,
            version = "test",
            bpm = "160",
        )

        val parsed = TextageParser.parseChart(chart, source)

        assertTrue(parsed.parsed)
        assertTrue(parsed.notes.any { it.lane == 8 })
    }

    @Test
    fun parsesTextageTempoChangesAndMeasureLengths() {
        val source = """
            genre="X"; title="X"; artist="Y"; bpm="40～165"; measure=5;
            ln[3]=192;
            tc[1]=[" 900"];
            tc[2]=["1100"," 8064"," 7096"];
            if(k){
                sp[1]="01";
            }else{
                sp[1]="01";
            }
        """.trimIndent()
        val chart = IidxChart(
            id = "tempo-test",
            title = "tempo-test",
            mode = "SP",
            difficulty = "H",
            level = 1,
            notes = 1,
            version = "test",
            bpm = "40～165",
        )

        val parsed = TextageParser.parseChart(chart, source)

        assertEquals(90f, parsed.bpm, 0.001f)
        assertEquals(4, parsed.bpmChanges.size)
        assertEquals(4f, parsed.bpmChanges[1].beat, 0.001f)
        assertEquals(2f, parsed.measureLengths[2], 0.001f)
        assertTrue(parsed.secondsAtBeat(5f) > parsed.secondsAtBeat(4f))
        assertTrue(parsed.scrollBeatAt(5f) > parsed.scrollBeatAt(4f))
    }
}
