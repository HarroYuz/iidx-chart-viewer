package com.harroyuz.iidxchartviewer.domain.model

import com.harroyuz.iidxchartviewer.domain.score.difficultyIndex

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

data class BjmUser(
    val id: String,
    val name: String,
    val email: String,
)
