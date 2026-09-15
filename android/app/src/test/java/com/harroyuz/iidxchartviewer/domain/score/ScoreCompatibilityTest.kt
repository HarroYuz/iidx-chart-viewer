package com.harroyuz.iidxchartviewer.domain.score

import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import org.junit.Assert.*
import org.junit.Test

class ScoreCompatibilityTest {
    private fun score(ex: Int = 1778) = BjmScore(1000, 0, 3, 5, 2, 100, ex, 0, 0)

    @Test fun usesActualGradeAndRoundsThresholdUp() {
        assertEquals("AA", djRate(1777, 1000))
        assertEquals("AAA", djRate(1778, 1000))
        assertEquals("AAA", djRate(2000, 1000))
        assertEquals("F", djRate(0, 1000))
        assertEquals("F", djRate(444, 1000))
        assertEquals("E", djRate(445, 1000))
    }

    @Test fun checksEveryGradeBoundary() {
        listOf("E", "D", "C", "B", "A", "AA", "AAA").forEachIndexed { index, grade ->
            val threshold = (2000 * (index + 2) + 8) / 9
            assertEquals(grade, djRate(threshold, 1000))
            assertNotEquals(grade, djRate(threshold - 1, 1000))
        }
    }

    @Test fun rejectsUnknownAndImpossibleScores() {
        assertFalse(score().matchesChartNotes(1000))
        assertFalse(score().withNoteReference(null).matchesChartNotes(1000))
        assertFalse(score(2001).withNoteReference(1000).matchesChartNotes(1000))
        assertFalse(score(-1).withNoteReference(1000).matchesChartNotes(1000))
        assertNull(djRate(100, 0))
    }

    @Test fun equalGradeIsInsufficientWhenVersionHasDifferentNotes() {
        val verified = score().withNoteReference(1000)
        assertTrue(verified.matchesChartNotes(1000))
        assertEquals(djRate(1778, 1000), djRate(1778, 999))
        assertFalse(verified.matchesChartNotes(999))
    }

    @Test fun rejectsContradictoryStoredGrade() {
        assertFalse(score().withNoteReference(1000).copy(sourceDjRate = "AA").matchesChartNotes(1000))
    }

    @Test fun handlesIntegerLimitsWithoutOverflow() {
        assertEquals("C", djRate(Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test fun backfillsIdenticalHistoryWithoutDuplicatingIt() {
        val old = score()
        val current = old.withNoteReference(1000)
        assertEquals(listOf(current), appendBjmHistory(listOf(old), listOf(current)))
    }

    @Test fun doesNotApplyCurrentReferenceToDifferentHistoricalAchievement() {
        val old = score()
        val current = old.copy(time = 200, exScore = 1800).withNoteReference(1000)
        assertEquals(listOf(current, old), appendBjmHistory(listOf(old), listOf(current)))
        val changedScore = old.copy(exScore = 1800).withNoteReference(1000)
        assertEquals(listOf(old), appendBjmHistory(listOf(old), listOf(changedScore)))
    }
}
