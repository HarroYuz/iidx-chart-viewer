package com.harroyuz.iidxchartviewer.domain.catalog

import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import org.junit.Assert.*
import org.junit.Test

class CatalogFilterTest {
    private val chart = IidxChart("a", "Song", mode = "SP", difficulty = "A", level = 12, notes = 1000, version = "33")
    private val allTypes = catalogSongTypes.toSet()

    @Test fun typeSelectionCannotRemoveItsLastItemButCanAddOtherTypes() {
        for (type in catalogSongTypes) {
            val only = setOf(type)
            assertEquals(only, toggleCatalogSongType(only, type))
            val other = catalogSongTypes.first { it != type }
            assertEquals(setOf(type, other), toggleCatalogSongType(only, other))
            assertEquals(setOf(other), toggleCatalogSongType(setOf(type, other), type))
        }
    }

    @Test fun typesUseRequestedOrderAndRecoverOldEmptySelections() {
        assertEquals(listOf(ArcadeStatus.CURRENT, ArcadeStatus.CONSUMER_ONLY, ArcadeStatus.DELETED), catalogSongTypes)
        assertEquals(allTypes - ArcadeStatus.DELETED, toggleCatalogSongType(emptySet(), ArcadeStatus.DELETED))
        assertEquals(allTypes, toggleCatalogSongType(emptySet(), ArcadeStatus.UNKNOWN))
    }

    @Test fun defaultsPreserveEveryStatusIncludingUnknownMetadata() {
        ArcadeStatus.entries.forEach {
            assertTrue(chart.copy(arcadeStatus = it).matchesCatalogFilters(emptySet(), emptySet(), allTypes))
        }
    }

    @Test fun songTypesArePositiveSelectionsAndDoNotMisclassifyUnknownMetadata() {
        for (selection in listOf(emptySet(), setOf(ArcadeStatus.CURRENT), setOf(ArcadeStatus.DELETED, ArcadeStatus.CONSUMER_ONLY))) {
            val actual = ArcadeStatus.entries.filter { chart.copy(arcadeStatus = it).matchesCatalogFilters(emptySet(), emptySet(), selection) }
            assertEquals(selection, actual.toSet())
        }
    }

    @Test fun selectionsUseUnionWithinEachDimensionAndIntersectionAcrossDimensions() {
        val current = chart.copy(arcadeStatus = ArcadeStatus.CURRENT)
        val versions = setOf("32", "33")
        val levels = setOf(11, 12)
        assertTrue(current.matchesCatalogFilters(versions, levels, allTypes))
        assertTrue(current.copy(version = "32", level = 11).matchesCatalogFilters(versions, levels, allTypes))
        assertFalse(current.copy(version = "31").matchesCatalogFilters(versions, levels, allTypes))
        assertFalse(current.copy(level = 10).matchesCatalogFilters(versions, levels, allTypes))
        assertFalse(current.matchesCatalogFilters(versions, levels, setOf(ArcadeStatus.CONSUMER_ONLY)))
    }

    @Test fun multipleFiltersMustMatchTheSameChartRatherThanDifferentChartsInTheSong() {
        val charts = listOf(chart.copy(version = "32", level = 10), chart.copy(version = "33", level = 12))
        assertTrue(charts.filter { it.matchesCatalogFilters(setOf("32"), setOf(12), allTypes) }.isEmpty())
        assertEquals(listOf(charts[1]), charts.filter { it.matchesCatalogFilters(setOf("32", "33"), setOf(12), allTypes) })
    }

    @Test fun versionsUseRequestedOrderAndAddNumbersOnlyAfterTenthStyle() {
        val samples = listOf(
            "Sparkle Shower" to "33", "Consumer only" to "0", "substream" to "s", "1st style" to "1",
            "10th style" to "10", "IIDX RED" to "11", "EPOLIS" to "31", "Pinky Crush" to "32",
        ).map { (version, directory) -> chart.copy(version = version, textageUrl = "https://textage.cc/score/$directory/song.html") }
        val options = buildCatalogVersionOptions(samples + samples.first())
        assertEquals(listOf("Consumer Only", "substream", "1st style", "10th style", "11 IIDX RED", "31 EPOLIS", "32 Pinky Crush", "33 Sparkle Shower"), options.map { it.label })
        assertEquals("Consumer only", options.first().value)
        assertEquals(listOf("CS", "sub", "1", "10", "11", "31", "32", "33"), options.map { it.abbreviation })
    }

    @Test fun unavailableChartUrlsDoNotDropVersionOptionsOrInventNumbers() {
        val options = buildCatalogVersionOptions(listOf(chart.copy(version = "substream"), chart.copy(version = "Consumer only"), chart.copy(version = "Legacy")))
        assertEquals(listOf("Consumer Only", "substream", "Legacy"), options.map { it.label })
        assertEquals(listOf("CS", "sub", "Legacy"), options.map { it.abbreviation })
    }

    @Test fun storedTextageVersionsOrderChartsEvenWhenPlaybackUrlsAreUnavailable() {
        val samples = listOf(chart.copy(version = "EPOLIS", textageVersion = 31),
            chart.copy(version = "substream", textageVersion = 35), chart.copy(version = "Consumer only", textageVersion = 0))
        assertEquals(listOf(-1, 0, 31), buildCatalogVersionOptions(samples).map { it.order })
    }
}
