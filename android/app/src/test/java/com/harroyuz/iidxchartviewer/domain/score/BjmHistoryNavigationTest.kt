package com.harroyuz.iidxchartviewer.domain.score

import com.harroyuz.iidxchartviewer.domain.catalog.buildSongGroups
import com.harroyuz.iidxchartviewer.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class BjmHistoryNavigationTest {
    private val chart = IidxChart("code-h", "CODE:Ø", mode = "SP", difficulty = "H", level = 9,
        notes = 1000, version = "Rootage", textageUrl = "https://textage.cc/score/26/code_0.html", arcadeStatus = ArcadeStatus.CURRENT)
    private val record = BjmScore(26016, 0, 2, 5, 2, 100, 1500, 0, 0)

    @Test fun legacyHistoryUsesTheSameConfirmedAliasAsTheCatalogWithoutAttachingAnUnverifiedScore() {
        val charts = listOf(chart, chart.copy(id = "code-n", difficulty = "N"), chart.copy(id = "code-dph", mode = "DP"))
        val state = IidxAppState(charts = charts, songGroups = buildSongGroups(charts), bjmMusic = listOf(BjmMusic(26016, "CODE:0")))
        val index = buildBjmIndex(state, 0, 0, 0)
        val candidates = buildBjmHistoryChartIndex(state, index)[record.key].orEmpty()
        val target = resolveBjmHistoryTarget(record, candidates)
        assertEquals(chart, target.navigationChart)
        assertNull(target.compatibleChart)
        assertFalse(record.matchesChartNotes(chart.notes))
    }

    @Test fun exactSymbolTitlesAlsoResolveBackFromHistoricalMusicIds() {
        val symbol = chart.copy(title = "∀")
        val state = IidxAppState(charts = listOf(symbol), songGroups = buildSongGroups(listOf(symbol)), bjmMusic = listOf(BjmMusic(28005, "∀")))
        val score = record.copy(musicId = 28005)
        val candidates = buildBjmHistoryChartIndex(state, buildBjmIndex(state, 0, 0, 0))[score.key].orEmpty()
        assertEquals(symbol, resolveBjmHistoryTarget(score, candidates).navigationChart)
    }

    @Test fun knownNoteMismatchCanOpenTheSongButCannotAttachTheHistoricalScore() {
        val score = record.withNoteReference(1100)
        val target = resolveBjmHistoryTarget(score, listOf(chart))
        assertEquals(chart, target.navigationChart)
        assertNull(target.compatibleChart)
        assertFalse(score.matchesChartNotes(chart.notes))
    }

    @Test fun matchingHistoricalVersionWinsOverAnIncompatibleCurrentRevision() {
        val old = chart.copy(id = "old", notes = 1100, arcadeStatus = ArcadeStatus.DELETED)
        val target = resolveBjmHistoryTarget(record.withNoteReference(1100), listOf(chart, old))
        assertEquals(old, target.navigationChart)
        assertEquals(old, target.compatibleChart)
    }

    @Test fun navigationNeverFallsBackToAnotherModeOrDifficulty() {
        val target = resolveBjmHistoryTarget(record, listOf(chart.copy(mode = "DP"), chart.copy(difficulty = "A")))
        assertNull(target.navigationChart)
        assertNull(target.compatibleChart)
        assertNull(resolveBjmHistoryTarget(record, emptyList()).navigationChart)
    }

    @Test fun unverifiedNavigationPrefersCurrentPlayableCharts() {
        val old = chart.copy(id = "old", arcadeStatus = ArcadeStatus.DELETED)
        assertEquals(chart, resolveBjmHistoryTarget(record, listOf(old, chart)).navigationChart)
    }
}
