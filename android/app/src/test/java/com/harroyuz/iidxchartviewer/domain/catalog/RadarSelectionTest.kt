package com.harroyuz.iidxchartviewer.domain.catalog

import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import org.junit.Assert.*
import org.junit.Test

class RadarSelectionTest {
    private val charts = listOf("N", "H", "A", "L").map { difficulty ->
        IidxChart(difficulty, "Song", mode = "SP", difficulty = difficulty, level = 10, notes = 1000, version = "33")
    }

    @Test fun historyEntryUsesItsRequestedDifficultyInEitherMode() {
        for (mode in listOf("SP", "DP")) for (difficulty in listOf("N", "H", "A", "L")) {
            val family = charts.map { it.copy(mode = mode) }
            assertEquals(difficulty, initialRadarChart(family, difficulty)?.difficulty)
            assertEquals(mode, initialRadarChart(family, difficulty)?.mode)
        }
    }

    @Test fun catalogEntryAndUnavailableHistoricalDifficultyKeepTheExistingDefault() {
        assertEquals("A", initialRadarChart(charts, null)?.difficulty)
        assertEquals("A", initialRadarChart(charts, "B")?.difficulty)
        assertEquals("H", initialRadarChart(charts.take(2), null)?.difficulty)
        assertNull(initialRadarChart(emptyList(), "H"))
    }
}
