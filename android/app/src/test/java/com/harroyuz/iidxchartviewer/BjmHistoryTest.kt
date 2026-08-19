package com.harroyuz.iidxchartviewer

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
