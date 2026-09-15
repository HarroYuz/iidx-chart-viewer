package com.harroyuz.iidxchartviewer.domain.catalog

import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import org.junit.Assert.*
import org.junit.Test

class CatalogFilterTest {
    private val chart = IidxChart("a", "Song", mode = "SP", difficulty = "A", level = 12, notes = 1000, version = "33")

    @Test fun defaultFilterPreservesEveryAvailabilityStatus() {
        ArcadeStatus.entries.forEach { assertTrue(chart.copy(arcadeStatus = it).matchesCatalogFilters(null, null, false)) }
    }

    @Test fun hideDeletedMatchesTheRedDeletionLabelAndRetainsUnknownCharts() {
        for (status in ArcadeStatus.entries) {
            assertEquals(!status.isUnavailable, chart.copy(arcadeStatus = status).matchesCatalogFilters(null, null, true))
        }
    }

    @Test fun deletionFilterCombinesWithVersionAndLevel() {
        val current = chart.copy(arcadeStatus = ArcadeStatus.CURRENT)
        assertTrue(current.matchesCatalogFilters("33", 12, true))
        assertFalse(current.matchesCatalogFilters("32", 12, true))
        assertFalse(current.matchesCatalogFilters("33", 11, true))
        assertFalse(current.copy(arcadeStatus = ArcadeStatus.DELETED).matchesCatalogFilters("33", 12, true))
    }

    @Test fun mixedSongKeepsItsCurrentCandidateWhileRemovedOnlySongDisappears() {
        val current = chart.copy(arcadeStatus = ArcadeStatus.CURRENT)
        val removed = chart.copy(id = "old", arcadeStatus = ArcadeStatus.DELETED)
        assertEquals(listOf(current), listOf(removed, current).filter { it.matchesCatalogFilters(null, null, true) })
        assertTrue(listOf(removed).filter { it.matchesCatalogFilters(null, null, true) }.isEmpty())
    }
}
