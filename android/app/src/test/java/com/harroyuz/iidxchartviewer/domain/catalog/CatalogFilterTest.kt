package com.harroyuz.iidxchartviewer.domain.catalog

import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import org.junit.Assert.*
import org.junit.Test

class CatalogFilterTest {
    private val chart = IidxChart("a", "Song", mode = "SP", difficulty = "A", level = 12, notes = 1000, version = "33")

    @Test fun defaultFilterPreservesEveryAvailabilityStatus() {
        ArcadeStatus.entries.forEach { assertTrue(chart.copy(arcadeStatus = it).matchesCatalogFilters(null, null, false, false)) }
    }

    @Test fun availabilityFiltersIndependentlyHideOnlyTheirOwnStatus() {
        val charts = ArcadeStatus.entries.map { chart.copy(arcadeStatus = it) }
        fun remaining(hideDeleted: Boolean, hideConsumer: Boolean) = charts
            .filter { it.matchesCatalogFilters(null, null, hideDeleted, hideConsumer) }
            .map { it.arcadeStatus }.toSet()
        assertEquals(setOf(ArcadeStatus.CURRENT, ArcadeStatus.UNKNOWN, ArcadeStatus.CONSUMER_ONLY), remaining(true, false))
        assertEquals(setOf(ArcadeStatus.CURRENT, ArcadeStatus.UNKNOWN, ArcadeStatus.DELETED), remaining(false, true))
        assertEquals(setOf(ArcadeStatus.CURRENT, ArcadeStatus.UNKNOWN), remaining(true, true))
    }

    @Test fun availabilityFiltersCombineWithVersionAndLevel() {
        val current = chart.copy(arcadeStatus = ArcadeStatus.CURRENT)
        assertTrue(current.matchesCatalogFilters("33", 12, true, true))
        assertFalse(current.matchesCatalogFilters("32", 12, true, true))
        assertFalse(current.matchesCatalogFilters("33", 11, true, true))
        assertFalse(current.copy(arcadeStatus = ArcadeStatus.DELETED).matchesCatalogFilters("33", 12, true, true))
        assertFalse(current.copy(arcadeStatus = ArcadeStatus.CONSUMER_ONLY).matchesCatalogFilters("33", 12, true, true))
    }

    @Test fun mixedSongKeepsItsCurrentCandidateWhileRemovedOnlySongDisappears() {
        val current = chart.copy(arcadeStatus = ArcadeStatus.CURRENT)
        val removed = chart.copy(id = "old", arcadeStatus = ArcadeStatus.DELETED)
        val consumer = chart.copy(id = "cs", arcadeStatus = ArcadeStatus.CONSUMER_ONLY)
        assertEquals(listOf(current), listOf(removed, consumer, current).filter { it.matchesCatalogFilters(null, null, true, true) })
        assertTrue(listOf(removed).filter { it.matchesCatalogFilters(null, null, true, true) }.isEmpty())
    }
}
