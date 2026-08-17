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
}
