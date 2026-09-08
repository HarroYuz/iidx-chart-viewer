package com.harroyuz.iidxchartviewer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartPlaybackIndexTest {
    @Test
    fun passedNoteCountTreatsHoldStartAndEndAsSeparateNotes() {
        val data = chartData(
            notes = listOf(
                ChartNote(beat = 1f, lane = 1),
                ChartNote(beat = 2f, lane = 2, holdBeats = 4f),
            ),
        )

        assertEquals(0, data.passedNoteCount(0f))
        assertEquals(1, data.passedNoteCount(1f))
        assertEquals(2, data.passedNoteCount(2f))
        assertEquals(3, data.passedNoteCount(6f))
        assertEquals(3, data.totalNoteCount())
    }

    @Test
    fun visibleNotesIncludesHoldThatStartedBeforeTheWindow() {
        val hold = ChartNote(beat = 0f, lane = 1, holdBeats = 12f)
        val data = chartData(
            notes = listOf(
                hold,
                ChartNote(beat = 11f, lane = 2),
                ChartNote(beat = 20f, lane = 3),
            ),
        )
        val visible = mutableListOf<ChartNote>()

        data.forEachVisibleNote(firstBeat = 10f, lastBeat = 15f) { visible += it }

        assertEquals(listOf(hold, ChartNote(11f, 2)), visible)
    }

    @Test
    fun bpmTimelineUsesCumulativeTimeAtEachBpmChange() {
        val data = chartData(
            notes = emptyList(),
            bpm = 120f,
            bpmChanges = listOf(
                BpmChange(0f, 120f),
                BpmChange(8f, 240f),
                BpmChange(12f, 60f),
            ),
        )

        assertEquals(120f, data.bpmAt(7.999f), 0.001f)
        assertEquals(240f, data.bpmAt(8f), 0.001f)
        assertEquals(4f, data.secondsAtBeat(8f), 0.001f)
        assertEquals(4.5f, data.secondsAtBeat(10f), 0.001f)
        assertEquals(5f, data.secondsAtBeat(12f), 0.001f)
        assertEquals(10f, data.beatAtSeconds(4.5f), 0.001f)
        assertEquals(12f, data.beatAtSeconds(5f), 0.001f)
        assertEquals(16f, data.beatAtSeconds(9f), 0.001f)
    }

    @Test
    fun bpmTimelineCanBeQueriedInEitherModeOrder() {
        val data = chartData(
            notes = emptyList(),
            bpm = 150f,
            bpmChanges = listOf(BpmChange(0f, 150f), BpmChange(16f, 75f)),
        )

        // The timeline is chart data, not player state. A mode switch during
        // playback must not invalidate or rebuild these values.
        val beat = 20f
        val seconds = data.secondsAtBeat(beat)
        assertTrue(seconds > 0f)
        assertEquals(beat, data.beatAtSeconds(seconds), 0.001f)
        assertEquals(3.2f, data.secondsAtBeat(8f), 0.001f)
        assertEquals(beat, data.beatAtSeconds(seconds), 0.001f)
    }

    @Test
    fun indexedVisibilityAndCountsMatchFullScanAcrossDenseChart() {
        val random = kotlin.random.Random(711)
        val notes = List(4000) { index ->
            ChartNote(random.nextInt(0, 2000) / 4f, index % 16,
                if (index % 5 == 0) random.nextInt(1, 160) / 4f else 0f)
        }
        val data = chartData(notes)
        val order = compareBy<ChartNote> { it.beat }.thenBy { it.lane }.thenBy { it.holdBeats }
        for (step in 0..120) {
            val first = step * 4f
            val last = first + 12f
            val actual = mutableListOf<ChartNote>()
            data.forEachVisibleNote(first, last) { actual += it }
            val expected = notes.filter { it.beat <= last && it.beat + it.holdBeats >= first }
            assertEquals("visible at $first", expected.sortedWith(order), actual.sortedWith(order))
            val passed = notes.sumOf {
                (if (it.beat <= first + .001f) 1 else 0) +
                    (if (it.holdBeats > 0f && it.beat + it.holdBeats <= first + .001f) 1 else 0)
            }
            assertEquals("count at $first", passed, data.passedNoteCount(first))
        }
        assertEquals(4800, data.totalNoteCount())
    }

    @Test
    fun emptyChartAndReversedWindowAreSupported() {
        val empty = chartData(emptyList())
        assertEquals(0, empty.totalNoteCount())
        empty.forEachVisibleNote(0f, 10f) { throw AssertionError("Unexpected note") }
        val note = ChartNote(4f, 1, 8f)
        val data = chartData(listOf(note))
        val visible = mutableListOf<ChartNote>()
        data.forEachVisibleNote(12f, 8f) { visible += it }
        assertEquals(listOf(note), visible)
        assertEquals(data.secondsAtBeat(4f), data.noteStartSeconds(note), .0001f)
        assertEquals(data.secondsAtBeat(12f), data.noteEndSeconds(note), .0001f)
    }

    private fun chartData(
        notes: List<ChartNote>,
        bpm: Float = 150f,
        bpmChanges: List<BpmChange> = listOf(BpmChange(0f, bpm)),
    ) = TextageChartData(
        chart = IidxChart(
            id = "test",
            title = "test",
            mode = "SP",
            difficulty = "A",
            level = 10,
            notes = 0,
            version = "test",
        ),
        notes = notes,
        durationBeats = 32f,
        parsed = true,
        bpm = bpm,
        bpmChanges = bpmChanges,
    )
}
