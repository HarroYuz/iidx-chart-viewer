package com.harroyuz.iidxchartviewer.domain.score

import com.harroyuz.iidxchartviewer.domain.catalog.buildSongGroups
import com.harroyuz.iidxchartviewer.domain.catalog.preferredChartOrder
import com.harroyuz.iidxchartviewer.domain.catalog.songGroupKey
import com.harroyuz.iidxchartviewer.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ArcadeChartPreferenceTest {
    private val current = IidxChart("current", "Song", mode = "SP", difficulty = "A", level = 12,
        notes = 1000, version = "33", textageUrl = "https://textage.cc/score/33/song.html", arcadeStatus = ArcadeStatus.CURRENT)
    private val removed = current.copy(id = "removed", version = "1", textageUrl = "https://textage.cc/score/1/song_old.html", arcadeStatus = ArcadeStatus.DELETED)
    private val score = BjmScore(42, 0, 3, 5, 1, 100, 1800, 0, 0).withNoteReference(1000)

    @Test fun prefersCurrentWhenBothChartsPassScoreChecks() {
        assertEquals(current, preferredChartForScore(score, listOf(removed, current)))
        assertEquals(current, preferredChartForScore(score, listOf(current, removed)))
    }

    @Test fun doesNotPreferIncompatibleCurrentChartOverCompatibleOldChart() {
        assertEquals(removed, preferredChartForScore(score, listOf(current.copy(notes = 1001), removed)))
        assertNull(preferredChartForScore(score, listOf(current.copy(notes = 1001))))
        assertNull(preferredChartForScore(score.copy(sourceNoteCount = null), listOf(current, removed)))
    }

    @Test fun prefersCurrentRatherThanLargerOldNoteCountInCatalog() {
        assertEquals(current, listOf(removed.copy(notes = 2000), current).maxWithOrNull(preferredChartOrder))
    }

    @Test fun choosesCurrentSongRepresentativeForBjmVersionMapping() {
        val charts = listOf(removed, current)
        val state = IidxAppState(charts = charts, songGroups = buildSongGroups(charts),
            bjmMusic = listOf(BjmMusic(41, "Song", version = 1), BjmMusic(42, "Song", version = 33)))
        assertEquals("33", state.songGroups.single().version)
        assertEquals(42, buildBjmIndex(state, 1, 2, 3).songMusicIds[songGroupKey(current)])
    }
}
