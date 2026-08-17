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

data class TextageChartData(
    val chart: IidxChart,
    val notes: List<ChartNote>,
    val durationBeats: Float,
    val parsed: Boolean,
    val bpm: Float = 150f,
    val parserMessage: String? = null,
)

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
