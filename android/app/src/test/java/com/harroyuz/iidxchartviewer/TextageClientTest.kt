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
    fun selectsDpHyperDefaultAfterBeginnerBranch() {
        val source = """
            if(k){
                sp[1]="01";
            }else{
                if(g){
                    dp[1]="02";
                }else{
                    sp[1]="01";
                    dp[1]="04";
                    if(a){
                        dp[1]="08";
                    }
                    if(l){
                        dp[1]="10";
                    }
                }
            }
        """.trimIndent()
        val chart = IidxChart(
            id = "test-dp-hyper-default",
            title = "test",
            mode = "DP",
            difficulty = "H",
            level = 7,
            notes = 1,
            version = "test",
            bpm = "140",
        )

        val parsed = TextageParser.parseChart(chart, source)

        assertTrue(parsed.parsed)
        assertTrue(parsed.notes.any { it.lane == 10 })
        assertTrue(parsed.notes.none { it.lane == 9 || it.lane == 11 })
    }

    @Test
    fun parsesTextageTempoChangesAndMeasureLengths() {
        val source = """
            genre="X"; title="X"; artist="Y"; bpm="40～165"; measure=5;
            for(ii=2;ii<=2;ii++)ln[ii]=192;
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
        assertEquals(6f, parsed.bpmChanges[2].beat, 0.001f)
        assertEquals(2f, parsed.measureLengths[1], 0.001f)
        assertEquals(2f, parsed.measureLengths[2], 0.001f)
        assertTrue(parsed.secondsAtBeat(5f) > parsed.secondsAtBeat(4f))
    }

    @Test
    fun decodesTextageXCompressionAndClearsInheritedCharges() {
        val source = """
            measure=3;
            if(k){
                c1[2]=[[0,0,32,3]];
                sp[2]="x07882@0404";
                if(a){
                    c1=[];
                    sp[2]="x07882@0404";
                }
            }else{
                sp[2]="";
            }
        """.trimIndent()
        val chart = IidxChart(
            id = "compression-test",
            title = "compression-test",
            mode = "SP",
            difficulty = "A",
            level = 1,
            notes = 2,
            version = "test",
            bpm = "160",
        )

        val parsed = TextageParser.parseChart(chart, source)

        assertTrue(parsed.parsed)
        assertTrue(parsed.notes.none { it.lane == 0 })
        assertTrue(parsed.notes.any { it.beat == 4f && it.lane == 1 })
    }

    @Test
    fun alignsRawTextageNotesWithTempoChangePosition() {
        val source = """
            measure=9;
            tc[9]=["1400","155112"];
            if(k){
                sp[9]="0000000000000057";
            }else{
                sp[9]="";
            }
        """.trimIndent()
        val chart = IidxChart(
            id = "tempo-note-alignment-test",
            title = "tempo-note-alignment-test",
            mode = "SP",
            difficulty = "H",
            level = 1,
            notes = 1,
            version = "test",
            bpm = "140",
        )

        val parsed = TextageParser.parseChart(chart, source)

        assertEquals(35.5f, parsed.notes.first().beat, 0.001f)
        assertEquals(35.5f, parsed.bpmChanges.last().beat, 0.001f)
    }

    @Test
    fun decodesCompressedScratchAsScratchLane() {
        val source = """
            measure=4;
            if(k){
                sp[4]="#XAA4AAA9d-Qw";
            }else{
                sp[4]="";
            }
        """.trimIndent()
        val chart = IidxChart(
            id = "compressed-scratch-test",
            title = "compressed-scratch-test",
            mode = "SP",
            difficulty = "H",
            level = 1,
            notes = 2,
            version = "test",
            bpm = "200",
        )

        val parsed = TextageParser.parseChart(chart, source)

        assertEquals(2, parsed.notes.count { it.lane == 0 })
        assertTrue(parsed.notes.none { it.lane == 1 })
    }

    @Test
    fun expandsChainedSparseAssignments() {
        val source = """
            measure=30;
            if(k){
                sp[27]="01";
                sp[28]=sp[29]=sp[27];
            }else{
                sp[27]="";
            }
        """.trimIndent()
        val chart = IidxChart(
            id = "chained-sparse-assignment-test",
            title = "chained-sparse-assignment-test",
            mode = "SP",
            difficulty = "H",
            level = 1,
            notes = 3,
            version = "test",
            bpm = "200",
        )

        val parsed = TextageParser.parseChart(chart, source)

        assertTrue(parsed.notes.any { it.lane == 0 && it.beat == 112f })
    }
}
