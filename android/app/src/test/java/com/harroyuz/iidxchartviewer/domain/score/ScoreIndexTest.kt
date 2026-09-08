package com.harroyuz.iidxchartviewer.domain.score

import com.harroyuz.iidxchartviewer.domain.catalog.buildSongGroups
import com.harroyuz.iidxchartviewer.domain.catalog.songGroupKey
import com.harroyuz.iidxchartviewer.domain.model.BjmMusic
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxAppState
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import org.junit.Assert.*
import org.junit.Test

class ScoreIndexTest {
    private val chart = IidxChart(
        id = "song-spa", title = "Ａ Song!", composer = "Artist", mode = "SP",
        difficulty = "A", level = 12, notes = 1000, version = "33",
        textageUrl = "https://textage.cc/score/33/song.html",
    )
    private val score = BjmScore(42, 0, 3, 5, 10, 1000, 1800, 0, 0)
    private fun state(charts: List<IidxChart> = listOf(chart)) = IidxAppState(
        charts = charts,
        songGroups = buildSongGroups(charts),
        bjmMusic = listOf(BjmMusic(42, "a song", version = 33, levels = listOf("0", "4", "8", "12", "0"))),
        bjmScores = listOf(score),
    )

    @Test fun groupsStylesAndDifficultiesButKeepsSourceVariantsSeparate() {
        val charts = listOf(chart, chart.copy(id = "song-dpa", mode = "DP"), chart.copy(id = "song-sph", difficulty = "H"), chart.copy(id = "song-remix", sourceLabel = "REMIX"))
        val groups = buildSongGroups(charts)
        assertEquals(2, groups.size)
        assertEquals(listOf("song-spa", "song-dpa", "song-sph"), groups.first().chartIds)
    }

    @Test fun matchesNormalizedTitlesAndIndexesScores() {
        val index = buildBjmIndex(state(), 1, 2, 3)
        assertEquals(42, index.songMusicIds[songGroupKey(chart)])
        assertEquals(score, index.scoresByKey["42:0:3"])
        assertTrue(isPersistedBjmIndexUsable(state(), index, 1, 2, 3))
    }

    @Test fun catalogRebuildPreservesScoreSnapshot() {
        val previous = buildBjmIndex(state(), 1, 2, 3)
        val updated = rebuildBjmCatalogIndex(state(), previous, 4, 5, 3)
        assertSame(previous.scoresByKey, updated.scoresByKey)
        assertEquals(4L, updated.textageRevision)
        assertEquals(5L, updated.musicRevision)
    }

    @Test fun scoreRebuildPreservesSongMapping() {
        val previous = buildBjmIndex(state(), 1, 2, 3)
        val changed = score.copy(exScore = 1900)
        val updated = rebuildBjmScoresIndex(state().copy(bjmScores = listOf(changed)), previous, 1, 2, 4)
        assertSame(previous.songMusicIds, updated.songMusicIds)
        assertEquals(changed, updated.scoresByKey[score.key])
    }

    @Test fun rejectsStaleOrCorruptPersistedIndexes() {
        val index = buildBjmIndex(state(), 1, 2, 3)
        assertFalse(isPersistedBjmIndexUsable(state(), index, 2, 2, 3))
        assertFalse(isPersistedBjmIndexUsable(state(), index, 1, 3, 3))
        assertFalse(isPersistedBjmIndexUsable(state(), index, 1, 2, 4))
        assertFalse(isPersistedBjmIndexUsable(state(), index.copy(built = false), 1, 2, 3))
        assertFalse(isPersistedBjmIndexUsable(state(), index.copy(scoresByKey = mapOf("wrong" to score)), 1, 2, 3))
        assertFalse(isPersistedBjmIndexUsable(state(), index.copy(songMusicIds = mapOf("" to 42)), 1, 2, 3))
    }
}
