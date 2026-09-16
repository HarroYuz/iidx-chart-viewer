package com.harroyuz.iidxchartviewer.domain.score

import com.harroyuz.iidxchartviewer.domain.catalog.catalogSongTypes
import com.harroyuz.iidxchartviewer.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class HistoryCatalogFilterTest {
    private val chart = IidxChart("song-h", "Song Name", subtitle = "Subtitle", composer = "Composer",
        genre = "TRANCE", mode = "SP", difficulty = "H", level = 9, notes = 1000, version = "EPOLIS", arcadeStatus = ArcadeStatus.CURRENT)
    private val music = BjmMusic(31001, "Song Name", "Plain Name", genre = "BJM Genre", artist = "BJM Artist")
    private val record = BjmScore(31001, 0, 2, 5, 0, 100, 1500, 0, 0)
    private val allTypes = catalogSongTypes.toSet()
    private val allFields = setOf("曲名", "曲师", "曲风")
    private fun matches(target: IidxChart? = chart, query: String = "", fields: Set<String> = allFields,
        versions: Set<String> = emptySet(), levels: Set<Int> = emptySet(), types: Set<ArcadeStatus> = allTypes) =
        matchesHistoryCatalogFilters(target, music, query, fields, versions, levels, types)

    @Test fun searchOnlyUsesSelectedFieldsAndKeepsTitleAliasesAndSubtitles() {
        assertTrue(matches(query = " subtitle ", fields = setOf("曲名")))
        assertTrue(matches(query = "plain", fields = setOf("曲名")))
        assertFalse(matches(query = "composer", fields = setOf("曲名")))
        assertTrue(matches(query = "composer", fields = setOf("曲师")))
        assertFalse(matches(query = "trance", fields = setOf("曲师")))
        assertTrue(matches(query = "trance", fields = setOf("曲风")))
    }

    @Test fun bjmArtistAndGenreRemainSearchableForUnlinkedHistory() {
        assertTrue(matches(target = null, query = "BJM Artist", fields = setOf("曲师")))
        assertTrue(matches(target = null, query = "BJM Genre", fields = setOf("曲风")))
        assertFalse(matches(target = null, query = "BJM Genre", fields = setOf("曲名")))
    }

    @Test fun unknownMetadataIsKeptByDefaultButCannotSatisfySpecificCatalogFilters() {
        assertTrue(matches(target = null))
        assertFalse(matches(target = null, levels = setOf(9)))
        assertFalse(matches(target = null, versions = setOf("EPOLIS")))
        assertFalse(matches(target = null, types = setOf(ArcadeStatus.CURRENT)))
        assertTrue(matches(chart.copy(arcadeStatus = ArcadeStatus.UNKNOWN)))
        assertFalse(matches(chart.copy(arcadeStatus = ArcadeStatus.UNKNOWN), types = setOf(ArcadeStatus.DELETED)))
    }

    @Test fun selectedVersionsAndLevelsAreUnionsButAllFilterDimensionsMustMatch() {
        assertTrue(matches(query = "song", versions = setOf("EPOLIS", "Rootage"), levels = setOf(8, 9)))
        assertFalse(matches(query = "missing", versions = setOf("EPOLIS"), levels = setOf(9)))
        assertFalse(matches(versions = setOf("Rootage"), levels = setOf(9)))
        assertFalse(matches(versions = setOf("EPOLIS"), levels = setOf(10)))
        assertFalse(matches(versions = setOf("EPOLIS"), levels = setOf(9), types = setOf(ArcadeStatus.DELETED)))
    }

    @Test fun levelFilterUsesTheRecordedDifficultyRatherThanAnotherChartOfTheSameSong() {
        val target = resolveBjmHistoryTarget(record, listOf(chart, chart.copy(difficulty = "A", level = 12))).navigationChart
        assertTrue(matches(target, levels = setOf(9)))
        assertFalse(matches(target, levels = setOf(12)))
    }

    @Test fun legacyRecordCanUseKnownCatalogMetadataWithoutAttachingAnUnverifiedScore() {
        val target = resolveBjmHistoryTarget(record, listOf(chart))
        assertNull(target.compatibleChart)
        assertTrue(matches(target.navigationChart, levels = setOf(9), versions = setOf("EPOLIS")))
        assertNull(record.sourceNoteCount)
        assertNull(record.sourceDjRate)
    }

    @Test fun historicalCompatibleRevisionDeterminesTheTypeFilter() {
        val old = chart.copy(id = "old", notes = 1100, arcadeStatus = ArcadeStatus.DELETED)
        val target = resolveBjmHistoryTarget(record.withNoteReference(1100), listOf(chart, old))
        assertTrue(matches(target.navigationChart, types = setOf(ArcadeStatus.DELETED)))
        assertFalse(matches(target.navigationChart, types = setOf(ArcadeStatus.CURRENT)))
    }
}
