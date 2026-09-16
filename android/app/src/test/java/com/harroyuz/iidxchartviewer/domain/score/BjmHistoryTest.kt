package com.harroyuz.iidxchartviewer.domain.score

import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import org.junit.Assert.assertEquals
import org.junit.Test

class BjmHistoryTest {
    @Test
    fun appendsOnlyRecordsAfterTheLatestStoredTime() {
        val old = score(musicId = 1, time = 100, exScore = 120)
        val newer = score(musicId = 1, time = 200, exScore = 130)
        val older = score(musicId = 2, time = 90, exScore = 80)

        val result = appendBjmHistory(listOf(old), listOf(older, newer))

        assertEquals(listOf(newer, old), result)
    }

    @Test
    fun doesNotDuplicateARecordWhenTheSameBestScoreIsSyncedAgain() {
        val record = score(musicId = 1, time = 100, exScore = 120)

        assertEquals(listOf(record), appendBjmHistory(listOf(record), listOf(record)))
    }

    @Test fun offlineBackfillOnlyEnrichesIdenticalRecordsAndPreservesOrderAndCount() {
        val old = score(1, 100, 120)
        val other = score(2, 90, 80)
        val existing = listOf(old, other)
        val result = refreshBjmHistoryReferences(existing, listOf(old.withNoteReference(100), other.copy(time = 200).withNoteReference(100)))
        assertEquals(listOf(old.withNoteReference(100), other), result)
        assertEquals(existing.map { it.time }, result.map { it.time })
    }

    @Test fun offlineBackfillRejectsChangedScoresOrUnverifiedReferences() {
        val old = score(1, 100, 120)
        assertEquals(listOf(old), refreshBjmHistoryReferences(listOf(old), listOf(old.copy(exScore = 121).withNoteReference(100))))
        assertEquals(listOf(old), refreshBjmHistoryReferences(listOf(old), listOf(old.withNoteReference(50))))
    }

    @Test fun knownAndContradictoryPartialHistoricalReferencesAreNotOverwritten() {
        val old = score(1, 100, 120)
        val known = old.withNoteReference(200)
        val partial = old.copy(sourceNoteCount = 200)
        val current = old.withNoteReference(100)
        assertEquals(listOf(known), refreshBjmHistoryReferences(listOf(known), listOf(current)))
        assertEquals(listOf(partial), refreshBjmHistoryReferences(listOf(partial), listOf(current)))
    }

    private fun score(musicId: Int, time: Long, exScore: Int) = BjmScore(
        musicId = musicId,
        playStyle = 0,
        noteId = 3,
        clearFlag = 5,
        missCount = 2,
        time = time,
        exScore = exScore,
        option1 = 0,
        option2 = 0,
    )
}
