package com.harroyuz.iidxchartviewer.domain.model

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
    // A newly started Textage sync reports 0/0 until the catalog tells us
    // its size. It must render as an empty progress bar, not as complete.
    val fraction: Float get() = if (total <= 0) 0f else (completed.toFloat() / total).coerceIn(0f, 1f)
}
