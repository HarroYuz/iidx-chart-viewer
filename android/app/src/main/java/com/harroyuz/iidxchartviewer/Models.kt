package com.harroyuz.iidxchartviewer

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
    private val effectiveBpmChanges: List<BpmChange>
        get() = (if (bpmChanges.isEmpty()) listOf(BpmChange(0f, bpm)) else bpmChanges)
            .sortedBy { it.beat }

    fun bpmAt(beat: Float): Float = effectiveBpmChanges
        .lastOrNull { it.beat <= beat + 0.0001f }
        ?.bpm
        ?.takeIf { it > 0f }
        ?: bpm

    /**
     * Converts musical beat position to the vertical scroll coordinate used
     * by the player. BPM changes stretch/compress later sections so a BPM
     * change is visible in the note spacing as well as in playback timing.
     */
    fun scrollBeatAt(beat: Float): Float {
        val target = beat.coerceAtLeast(0f)
        val baseBpm = bpm.coerceAtLeast(1f)
        var position = 0f
        var segmentStart = 0f
        var segmentBpm = baseBpm
        effectiveBpmChanges.dropWhile { it.beat <= 0f }.forEach { change ->
            if (change.beat >= target) return@forEach
            position += (change.beat - segmentStart).coerceAtLeast(0f) * segmentBpm / baseBpm
            segmentStart = change.beat
            segmentBpm = change.bpm.coerceAtLeast(1f)
        }
        position += (target - segmentStart).coerceAtLeast(0f) * segmentBpm / baseBpm
        return position
    }

    fun beatAtScrollBeat(scrollBeat: Float): Float {
        val target = scrollBeat.coerceAtLeast(0f)
        var low = 0f
        var high = durationBeats.coerceAtLeast(4f)
        repeat(32) {
            val middle = (low + high) / 2f
            if (scrollBeatAt(middle) < target) low = middle else high = middle
        }
        return high.coerceIn(0f, durationBeats.coerceAtLeast(4f))
    }

    fun secondsAtBeat(beat: Float): Float {
        val target = beat.coerceAtLeast(0f)
        val changes = effectiveBpmChanges
        var elapsed = 0f
        var segmentStart = 0f
        var segmentBpm = changes.firstOrNull()?.bpm?.takeIf { it > 0f } ?: bpm
        changes.dropWhile { it.beat <= 0f }.forEach { change ->
            if (change.beat >= target) return@forEach
            elapsed += (change.beat - segmentStart).coerceAtLeast(0f) * 60f / segmentBpm.coerceAtLeast(1f)
            segmentStart = change.beat
            segmentBpm = change.bpm.coerceAtLeast(1f)
        }
        elapsed += (target - segmentStart).coerceAtLeast(0f) * 60f / segmentBpm.coerceAtLeast(1f)
        return elapsed
    }

    fun beatAtSeconds(seconds: Float): Float {
        var remaining = seconds.coerceAtLeast(0f)
        var beat = 0f
        var segmentBpm = effectiveBpmChanges.firstOrNull()?.bpm?.takeIf { it > 0f } ?: bpm
        effectiveBpmChanges.dropWhile { it.beat <= 0f }.forEach { change ->
            val segmentBeats = (change.beat - beat).coerceAtLeast(0f)
            val segmentSeconds = segmentBeats * 60f / segmentBpm.coerceAtLeast(1f)
            if (remaining <= segmentSeconds) return beat + remaining * segmentBpm / 60f
            remaining -= segmentSeconds
            beat = change.beat
            segmentBpm = change.bpm.coerceAtLeast(1f)
        }
        return beat + remaining * segmentBpm / 60f
    }

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

data class BjmScore(
    val musicId: Int,
    val playStyle: Int,
    val noteId: Int,
    val clearFlag: Int,
    val missCount: Int,
    val time: Long,
    val exScore: Int,
    val option1: Long,
    val option2: Long,
) {
    val key: String get() = "$musicId:$playStyle:$noteId"
}

data class BjmUser(
    val id: String,
    val name: String,
    val email: String,
)

data class IidxAppState(
    val charts: List<IidxChart> = emptyList(),
    val bjmScores: List<BjmScore> = emptyList(),
    val bjmUser: BjmUser? = null,
    val bjmSyncedAt: Long? = null,
) {
    val confirmedCount: Int get() = charts.count { it.confirmed }
    val playedCount: Int get() = bjmScores.size.takeIf { it > 0 } ?: charts.count { it.score != null }
}


data class TextageSyncProgress(
    val initial: Boolean,
    val completed: Int,
    val total: Int,
    val currentTitle: String,
    val failed: Int = 0,
) {
    val fraction: Float get() = if (total <= 0) 1f else (completed.toFloat() / total).coerceIn(0f, 1f)
}

data class PlayerSettings(
    val speed: Int = 1,
    val showBarLines: Boolean = true,
    val side: String = "1P",
    val mirror: Boolean = false,
) {
    val safeSpeed: Int get() = speed.coerceIn(1, 50)
}
