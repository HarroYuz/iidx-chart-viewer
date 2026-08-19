package com.harroyuz.iidxchartviewer

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextageClientTest {
    @Test
    fun buildsTextageChartUrlWithSideDifficultyAndLevel() {
        val spChart = IidxChart(
            id = "thearth8-spa",
            title = "THE EARTH LIGHT",
            mode = "SP",
            difficulty = "H",
            level = 7,
            notes = 1,
            version = "substream",
        )
        val dpChart = spChart.copy(id = "thearth8-dpa", mode = "DP", difficulty = "A", level = 12)

        assertEquals(
            "https://textage.cc/score/s/thearth8.html?1H700",
            buildTextageChartUrl("https://textage.cc/score/s/thearth8.html", spChart),
        )
        assertEquals(
            "https://textage.cc/score/s/thearth8.html?DAC00",
            buildTextageChartUrl("https://textage.cc/score/s/thearth8.html?old", dpChart),
        )
    }

    @Test
    fun executesTextageScriptsBeforeReadingChartArrays() {
        val source = """
            <script>
                if(k){
                    sp[1] = "01";
                    sp[2] = sp[1];
                } else {
                    sp[1] = "02";
                }
            </script>
        """.trimIndent()
        val chart = IidxChart(
            id = "js-engine-test",
            title = "test",
            mode = "SP",
            difficulty = "H",
            level = 1,
            notes = 0,
            version = "test",
            bpm = "160",
        )

        val parsed = TextageParser.parseChart(
            chart = chart,
            source = source,
            externalScripts = listOf("ln=[];tc=[];c1=[];c2=[];sp=[];dp=[];k=1;notes=2;measure=2;"),
            pageUrl = "https://textage.cc/score/0/test.html?1H700",
        )

        assertEquals(2, parsed.chart.notes)
        assertEquals(2, parsed.notes.size)
    }

    @Test
    fun usesOfficialStatisticsEventsForNotePositions() {
        val source = "<script>sp[1] = \"01\";</script>"
        val chart = IidxChart(
            id = "js-events-test",
            title = "test",
            mode = "SP",
            difficulty = "H",
            level = 1,
            notes = 0,
            version = "test",
            bpm = "160",
        )
        val officialStub = """
            ln=[]; tc=[]; c1=[]; c2=[]; sp=[]; dp=[]; notes=2; measure=2;
            function stat_insert() {}
            function b() {
                stat_insert(0, 1, 0, 0);
                stat_insert(0, 2, 384, 96);
            }
        """.trimIndent()

        val parsed = TextageParser.parseChart(
            chart = chart,
            source = source,
            externalScripts = listOf(officialStub),
            pageUrl = "https://textage.cc/score/0/test.html?1H700",
        )

        assertEquals(2, parsed.notes.size)
        assertEquals(0f, parsed.notes[0].beat, 0.001f)
        assertEquals(4f, parsed.notes[1].beat, 0.001f)
        assertEquals(1f, parsed.notes[1].holdBeats, 0.001f)
    }

    @Test
    fun alignsOfficialStatisticsWithDisplayedMeasureNumbers() {
        val source = "<script>sp[2] = \"01\";</script>"
        val chart = IidxChart(
            id = "official-measure-alignment-test",
            title = "test",
            mode = "SP",
            difficulty = "H",
            level = 1,
            notes = 1,
            version = "test",
            bpm = "160",
        )
        val officialStub = """
            ln=[384,384,384]; tc=[]; c1=[]; c2=[]; sp=[]; dp=[]; notes=1; measure=3;
            function stat_insert() {}
            function b() {
                stat_insert(0, 1, 768, 0);
            }
        """.trimIndent()

        val parsed = TextageParser.parseChart(
            chart = chart,
            source = source,
            externalScripts = listOf(officialStub),
            pageUrl = "https://textage.cc/score/0/test.html?1H700",
        )

        assertEquals(1, parsed.notes.size)
        assertEquals(4f, parsed.notes.single().beat, 0.001f)
    }

    @Test
    fun groupsUnavailableTextageDifficultyWithItsSong() {
        val available = IidxChart(
            id = "textage-carapain-sph",
            title = "Caramel Pain",
            mode = "SP",
            difficulty = "H",
            level = 6,
            notes = 525,
            version = "Sparkle Shower",
            textageUrl = "https://textage.cc/score/33/carapain.html",
        )
        val unavailable = available.copy(
            id = "textage-carapain-spn",
            difficulty = "N",
            level = 3,
            notes = 0,
            textageUrl = null,
        )

        assertEquals(chartSongKey(available), chartSongKey(unavailable))
        assertEquals("carapain", chartSongKey(unavailable))
        assertEquals(songGroupKey(available), songGroupKey(unavailable))
        assertNotEquals(songGroupKey(available), songGroupKey(available.copy(sourceLabel = "(CS8th)")))
    }

    @Test
    fun parsesTextageSourceLabelAndSubstreamUrl() = runBlocking {
        val versions = (0 until 36).joinToString(",") { "\"v$it\"" }
        val source = """
            SS=36;
            titletbl={
                'thearth8':[SS,1,1,"TRANCE","L.E.D. LIGHT","THE EARTH LIGHT"],
            };
            datatbl={
                'thearth8':[0,0,398,530,725,0,0,456,718,581,0,"145"],
            };
            actbl={
                'thearth8':[0,0,0,0,0,4,7,7,7,A,3,0,0,0,0,5,7,A,7,7,7,0,0,"(CS8th)"],
            };
            vertbl=[$versions];
            vertbl[36]="substream";
        """.trimIndent()

        val charts = TextageParser.parseCatalog(source) { _, _, _ -> }
        val chart = charts.first { it.mode == "SP" && it.difficulty == "A" }

        assertEquals("(CS8th)", chart.sourceLabel)
        assertEquals("substream", chart.version)
        assertTrue(chart.textageUrl!!.startsWith("https://textage.cc/score/s/thearth8.html"))
    }

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
    fun expandsTwoDigitChargeLanes() {
        val source = """
            measure=51;
            if(k){
                c1[51]=[[37,0,22],[26,24,22],[15,48]];
                sp[51]="";
            }else{
                sp[51]="";
            }
        """.trimIndent()
        val chart = IidxChart(
            id = "two-digit-charge-test",
            title = "two-digit-charge-test",
            mode = "SP",
            difficulty = "H",
            level = 1,
            notes = 6,
            version = "test",
            bpm = "160",
        )

        val parsed = TextageParser.parseChart(chart, source)

        assertEquals(listOf(3, 7, 2, 6, 1, 5), parsed.notes.map { it.lane })
        assertEquals(6, parsed.notes.size)
        assertTrue(parsed.notes.none { it.lane >= 8 })
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
    fun selectsTextageNormalAndLegendariaBranches() {
        val source = """
            measure=1;
            if(k){
                sp[1]="01";
                if(a){
                    sp[1]="02";
                    if(kuro){
                        sp[1]="04";
                    }
                }
                if(l){
                    sp[1]="08";
                }
            }else{
                dp[1]="01";
            }
        """.trimIndent()
        val normal = IidxChart(
            id = "textage-normal-branch",
            title = "textage-normal-branch",
            mode = "SP",
            difficulty = "N",
            level = 1,
            notes = 1,
            version = "test",
            bpm = "160",
        )
        val legendaria = normal.copy(
            id = "textage-legendaria-branch",
            difficulty = "L",
        )

        assertEquals(listOf(3), TextageParser.parseChart(normal, source).notes.map { it.lane })
        assertEquals(listOf(2), TextageParser.parseChart(legendaria, source).notes.map { it.lane })
    }

    @Test
    fun labelsBelowNextGradeAsTheCurrentGrade() {
        assertEquals("AA - 6", rankSummary(150, 100))
        assertEquals("AAA - 8", rankSummary(170, 100))
        assertEquals("A", listScoreRankName(150, 100))
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

    @Test
    fun decodesFascinationMaxxDpPatternUsedByBothMeasures() {
        val source = """
            measure=39;
            if(k){
                sp[1]="";
            }else{
                dp[37]="#p3P7xpppppppp";
                dp[39]="#p3P7xpppppppp";
            }
        """.trimIndent()
        val chart = IidxChart(
            id = "fas-maxx-dp-a",
            title = "Fascination MAXX",
            mode = "DP",
            difficulty = "A",
            level = 12,
            notes = 1313,
            version = "14",
            bpm = "100~400",
        )
        val parsed = TextageParser.parseChart(chart, source)

        val expectedLanes = listOf(15, 13, 11, 9).let { lanes ->
            List(8) { lanes }.flatten()
        }
        listOf(37, 39).forEach { measure ->
            val start = parsed.measureStart(measure)
            val end = parsed.measureEnd(measure)
            val notes = parsed.notes.filter { it.beat >= start && it.beat < end }
            assertEquals(expectedLanes.size, notes.size)
            assertEquals(expectedLanes, notes.map { it.lane })
        }
    }

    @Test
    fun keepsDp2pKeysInSourceOrderBeforeRendering() {
        assertEquals(8, dpDisplayLane(rawLane = 9, destinationKeyLane = 1))
        assertEquals(14, dpDisplayLane(rawLane = 15, destinationKeyLane = 7))
        assertEquals(15, dpDisplayLane(rawLane = 8, destinationKeyLane = 0))
    }
}
