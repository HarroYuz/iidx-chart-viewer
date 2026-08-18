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

    fun bpmAt(beat: Float): Float {
        val changes = effectiveBpmChanges
        for (index in changes.lastIndex downTo 0) {
            val change = changes[index]
            if (change.beat <= beat + 0.0001f) return change.bpm.takeIf { it > 0f } ?: bpm
        }
        return bpm
    }

    fun secondsAtBeat(beat: Float): Float {
        val target = beat.coerceAtLeast(0f)
        val changes = effectiveBpmChanges
        var elapsed = 0f
        var segmentStart = 0f
        var segmentBpm = changes.firstOrNull()?.bpm?.takeIf { it > 0f } ?: bpm
        for (change in changes) {
            if (change.beat <= 0f) {
                segmentBpm = change.bpm.coerceAtLeast(1f)
                continue
            }
            if (change.beat >= target) break
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
        for (change in effectiveBpmChanges) {
            if (change.beat <= 0f) {
                segmentBpm = change.bpm.coerceAtLeast(1f)
                continue
            }
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

data class BjmMusic(
    val musicId: Int,
    val title: String,
    val plainTitle: String = "",
    val genre: String = "",
    val artist: String = "",
    val version: Int = 0,
    val levels: List<String> = emptyList(),
) {
    fun level(mode: String, difficulty: String): Int {
        val styleIndex = if (mode == "DP") 1 else 0
        val difficultyIndex = when (difficulty) {
            "B" -> 0
            "N" -> 1
            "H" -> 2
            "A" -> 3
            "L" -> 4
            else -> return 0
        }
        return levels.getOrNull(styleIndex * 5 + difficultyIndex)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: 0
    }
}

data class BjmIndex(
    val songMusicIds: Map<String, Int> = emptyMap(),
    val scoresByKey: Map<String, BjmScore> = emptyMap(),
    val textageRevision: Long = 0L,
    val musicRevision: Long = 0L,
    val scoresRevision: Long = 0L,
    val built: Boolean = false,
)

data class IidxSongGroup(
    val key: String,
    val title: String,
    val subtitle: String = "",
    val genre: String = "",
    val composer: String = "",
    val version: String = "",
    val sourceLabel: String = "",
    val chartIds: List<String> = emptyList(),
)

data class BjmUser(
    val id: String,
    val name: String,
    val email: String,
)

data class IidxAppState(
    val charts: List<IidxChart> = emptyList(),
    val songGroups: List<IidxSongGroup> = emptyList(),
    val bjmScores: List<BjmScore> = emptyList(),
    val bjmMusic: List<BjmMusic> = emptyList(),
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
    val showBpmChanges: Boolean = true,
    val showMeasureNumbers: Boolean = true,
    val side: String = "1P",
    val playOption: String = "NONE",
    val playOption1P: String = "NONE",
    val playOption2P: String = "NONE",
    val randomMapping1P: List<Int> = (1..7).toList(),
    val randomMapping2P: List<Int> = (1..7).toList(),
) {
    val safeSpeed: Int get() = speed.coerceIn(1, 100)
    val safePlayOption: String get() = playOption.takeIf { it == "MIRROR" || it == "RANDOM" } ?: "NONE"
    val safePlayOption1P: String get() = playOption1P.takeIf { it == "MIRROR" || it == "RANDOM" } ?: "NONE"
    val safePlayOption2P: String get() = playOption2P.takeIf { it == "MIRROR" || it == "RANDOM" } ?: "NONE"
    val safeRandomMapping1P: List<Int> get() = normalizeRandomMapping(randomMapping1P)
    val safeRandomMapping2P: List<Int> get() = normalizeRandomMapping(randomMapping2P)
}

private fun normalizeRandomMapping(mapping: List<Int>): List<Int> {
    val valid = mapping.filter { it in 1..7 }.distinct().toMutableList()
    (1..7).forEach { value -> if (value !in valid) valid += value }
    return valid.take(7)
}
