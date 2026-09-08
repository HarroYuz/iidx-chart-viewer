package com.harroyuz.iidxchartviewer.domain.model

import java.util.IdentityHashMap

data class IidxChart(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val genre: String = "",
    val composer: String = "",
    val bpm: String = "",
    val mode: String,
    val difficulty: String,
    val level: Int,
    val notes: Int,
    val version: String,
    val sourceLabel: String = "",
    val score: Int? = null,
    val confirmed: Boolean = false,
    val textageUrl: String? = null,
)

data class ChartNote(
    val beat: Float,
    val lane: Int,
    val holdBeats: Float = 0f,
)

data class BpmChange(
    val beat: Float,
    val bpm: Float,
)

data class TextageChartData(
    val chart: IidxChart,
    val notes: List<ChartNote>,
    val durationBeats: Float,
    val parsed: Boolean,
    val bpm: Float = 150f,
    val bpmChanges: List<BpmChange> = emptyList(),
    // Each value is the musical length of one measure in quarter-note beats.
    // Textage's default is 4.0; ln[n] can override individual measures.
    val measureLengths: List<Float> = emptyList(),
    val parserMessage: String? = null,
) {
    private val effectiveBpmChanges: List<BpmChange> by lazy {
        (if (bpmChanges.isEmpty()) listOf(BpmChange(0f, bpm)) else bpmChanges)
            .sortedBy { it.beat }
    }

    /**
     * Chart-time lookup is independent of the selected player mode.  The
     * player can switch between beat-based Hi-Speed and time-based
     * Fixed-Speed while it is playing, so this cache must never contain a
     * mode-specific value.
     */
    private val bpmTimeline: BpmTimeline by lazy {
        BpmTimeline(effectiveBpmChanges, bpm)
    }

    private val noteIndex: ChartNoteIndex by lazy {
        ChartNoteIndex(notes)
    }

    private val noteTimes: IdentityHashMap<ChartNote, ChartNoteTimes> by lazy {
        IdentityHashMap<ChartNote, ChartNoteTimes>(notes.size).apply {
            notes.forEach { note ->
                put(
                    note,
                    ChartNoteTimes(
                        startSeconds = secondsAtBeat(note.beat),
                        endSeconds = secondsAtBeat(note.beat + note.holdBeats),
                    ),
                )
            }
        }
    }

    internal val positiveBpmChanges: List<BpmChange> by lazy {
        effectiveBpmChanges.filter { it.beat > 0f }
    }

    fun bpmAt(beat: Float): Float {
        return bpmTimeline.bpmAt(beat, bpm)
    }

    fun secondsAtBeat(beat: Float): Float {
        return bpmTimeline.secondsAtBeat(beat)
    }

    fun beatAtSeconds(seconds: Float): Float {
        return bpmTimeline.beatAtSeconds(seconds)
    }

    fun passedNoteCount(currentBeat: Float): Int = noteIndex.passedNoteCount(currentBeat)

    fun totalNoteCount(): Int = noteIndex.totalNoteCount

    fun noteStartSeconds(note: ChartNote): Float = noteTimes[note]?.startSeconds ?: secondsAtBeat(note.beat)

    fun noteEndSeconds(note: ChartNote): Float = noteTimes[note]?.endSeconds
        ?: secondsAtBeat(note.beat + note.holdBeats)

    fun forEachVisibleNote(
        firstBeat: Float,
        lastBeat: Float,
        action: (ChartNote) -> Unit,
    ) = noteIndex.forEachVisible(firstBeat, lastBeat, action)

    fun measureStart(index: Int): Float {
        if (index <= 1 || measureLengths.isEmpty()) return (index - 1).coerceAtLeast(0) * 4f
        return measureLengths.take((index - 1).coerceAtMost(measureLengths.size)).sum()
    }

    fun measureEnd(index: Int): Float = measureStart(index) +
        if (measureLengths.isEmpty()) 4f else measureLengths.getOrElse(index - 1) { 4f }

    fun measureAt(beat: Float): Int {
        if (measureLengths.isEmpty()) return (kotlin.math.floor(beat.coerceAtLeast(0f) / 4f).toInt() + 1)
        var start = 0f
        measureLengths.forEachIndexed { index, length ->
            if (beat < start + length) return index + 1
            start += length
        }
        return measureLengths.size.coerceAtLeast(1)
    }

    fun measureCount(): Int = measureLengths.size.takeIf { it > 0 } ?:
        kotlin.math.ceil(durationBeats.coerceAtLeast(4f) / 4f).toInt()
}

private class BpmTimeline(
    changes: List<BpmChange>,
    fallbackBpm: Float,
) {
    private val baseBpm = initialSegmentBpm(changes, fallbackBpm)
    private val displayBaseBpm = changes.lastOrNull { it.beat <= 0f }?.bpm
        ?.takeIf { it > 0f }
        ?: fallbackBpm
    private val changeBeats: FloatArray
    private val changeSeconds: FloatArray
    private val changeBpms: FloatArray
    private val displayBpms: FloatArray

    init {
        val positiveChanges = changes.filter { it.beat > 0f }
        changeBeats = FloatArray(positiveChanges.size)
        changeSeconds = FloatArray(positiveChanges.size)
        changeBpms = FloatArray(positiveChanges.size)
        displayBpms = FloatArray(positiveChanges.size)

        var segmentStartBeat = 0f
        var segmentStartSeconds = 0f
        var segmentBpm = baseBpm
        positiveChanges.forEachIndexed { index, change ->
            val beat = change.beat
            segmentStartSeconds += (beat - segmentStartBeat).coerceAtLeast(0f) * 60f / segmentBpm
            changeBeats[index] = beat
            changeSeconds[index] = segmentStartSeconds
            segmentBpm = change.bpm.coerceAtLeast(1f)
            changeBpms[index] = segmentBpm
            displayBpms[index] = change.bpm.takeIf { it > 0f } ?: fallbackBpm
            segmentStartBeat = beat
        }
    }

    fun bpmAt(beat: Float, fallbackBpm: Float): Float {
        val target = beat.coerceAtLeast(0f)
        val index = upperBound(changeBeats, target + 0.0001f) - 1
        return if (index >= 0) displayBpms[index] else displayBaseBpm
    }

    fun secondsAtBeat(beat: Float): Float {
        val target = beat.coerceAtLeast(0f)
        val index = upperBound(changeBeats, target) - 1
        if (index < 0) return target * 60f / baseBpm
        return changeSeconds[index] +
            (target - changeBeats[index]).coerceAtLeast(0f) * 60f / changeBpms[index]
    }

    fun beatAtSeconds(seconds: Float): Float {
        val target = seconds.coerceAtLeast(0f)
        val index = upperBound(changeSeconds, target) - 1
        if (index < 0) return target * baseBpm / 60f
        return changeBeats[index] +
            (target - changeSeconds[index]).coerceAtLeast(0f) * changeBpms[index] / 60f
    }
}

private fun initialSegmentBpm(changes: List<BpmChange>, fallbackBpm: Float): Float {
    var result = changes.firstOrNull()?.bpm?.takeIf { it > 0f }
        ?: fallbackBpm.coerceAtLeast(1f)
    changes.forEach { change ->
        if (change.beat <= 0f) result = change.bpm.coerceAtLeast(1f)
    }
    return result
}

private data class ChartNoteTimes(
    val startSeconds: Float,
    val endSeconds: Float,
)

private class ChartNoteIndex(notes: List<ChartNote>) {
    private val notesByBeat = notes.sortedWith(compareBy<ChartNote> { it.beat }.thenBy { it.lane })
    private val noteBeats = FloatArray(notesByBeat.size) { notesByBeat[it].beat }
    private val noteCountBeats = buildNoteCountBeats(notesByBeat)
    val totalNoteCount: Int get() = noteCountBeats.size
    private val holdNotes = notesByBeat.filter { it.holdBeats > 0f }
    private val holdStartBeats = FloatArray(holdNotes.size) { holdNotes[it].beat }
    private val holdTreeSize = nextPowerOfTwo(holdNotes.size)
    private val holdEndTree = buildHoldEndTree(holdNotes, holdTreeSize)

    fun passedNoteCount(currentBeat: Float): Int = upperBound(
        noteCountBeats,
        currentBeat + 0.001f,
    )

    fun forEachVisible(firstBeat: Float, lastBeat: Float, action: (ChartNote) -> Unit) {
        val lower = firstBeat.coerceAtMost(lastBeat)
        val upper = lastBeat.coerceAtLeast(firstBeat)
        // A hold that started before the visible window still needs its body
        // drawn until its tail passes the lower edge of the window.
        val priorHoldCount = lowerBound(holdStartBeats, lower)
        visitActiveHolds(
            node = 1,
            start = 0,
            end = holdTreeSize,
            limit = priorHoldCount,
            lowerBeat = lower,
            action = action,
        )

        val first = lowerBound(noteBeats, lower)
        val last = upperBound(noteBeats, upper)
        for (index in first until last) action(notesByBeat[index])
    }

    private fun visitActiveHolds(
        node: Int,
        start: Int,
        end: Int,
        limit: Int,
        lowerBeat: Float,
        action: (ChartNote) -> Unit,
    ) {
        if (start >= limit || holdEndTree[node] < lowerBeat) return
        if (end - start == 1) {
            if (start < holdNotes.size) action(holdNotes[start])
            return
        }
        val middle = (start + end) ushr 1
        visitActiveHolds(node * 2, start, middle, limit, lowerBeat, action)
        visitActiveHolds(node * 2 + 1, middle, end, limit, lowerBeat, action)
    }
}

private fun buildNoteCountBeats(notes: List<ChartNote>): FloatArray {
    val events = ArrayList<Float>(notes.sumOf { if (it.holdBeats > 0f) 2 else 1 })
    notes.forEach { note ->
        events += note.beat
        if (note.holdBeats > 0f) events += note.beat + note.holdBeats
    }
    return events.sorted().toFloatArray()
}

private fun lowerBound(values: FloatArray, target: Float): Int {
    var low = 0
    var high = values.size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (values[middle] < target) low = middle + 1 else high = middle
    }
    return low
}

private fun upperBound(values: FloatArray, target: Float): Int {
    var low = 0
    var high = values.size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (values[middle] <= target) low = middle + 1 else high = middle
    }
    return low
}

private fun nextPowerOfTwo(value: Int): Int {
    var result = 1
    while (result < value.coerceAtLeast(1)) result = result shl 1
    return result
}

private fun buildHoldEndTree(holds: List<ChartNote>, treeSize: Int): FloatArray {
    val tree = FloatArray(treeSize * 2) { Float.NEGATIVE_INFINITY }
    holds.forEachIndexed { index, hold -> tree[treeSize + index] = hold.beat + hold.holdBeats }
    for (node in treeSize - 1 downTo 1) tree[node] = maxOf(tree[node * 2], tree[node * 2 + 1])
    return tree
}
