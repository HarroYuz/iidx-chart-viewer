package com.harroyuz.iidxchartviewer.domain.player

internal const val PLAYER_SPEED_MODE_FLOATING = "FLOATING_HI_SPEED"
internal const val PLAYER_SPEED_MODE_HI = "HI_SPEED"
internal const val PLAYER_GREEN_NUMBER_MIN = 10
internal const val PLAYER_GREEN_NUMBER_MAX = 9999
internal const val PLAYER_GREEN_NUMBER_DEFAULT = 500

/**
 * Converts the player's speed setting into musical pixels per beat. Floating
 * Hi-Speed uses IIDX's green-number convention: 10 green-number units equal
 * one 60 Hz frame, so the visible travel time is greenNumber / 600 seconds at
 * the chart's starting BPM.
 */
internal fun playerPixelsPerBeat(
    speedMode: String,
    speed: Int,
    greenNumber: Int,
    initialBpm: Float,
    judgeDistancePx: Float,
): Float = if (speedMode == PLAYER_SPEED_MODE_FLOATING) {
    judgeDistancePx.coerceAtLeast(1f) * 36_000f /
        (greenNumber.coerceIn(PLAYER_GREEN_NUMBER_MIN, PLAYER_GREEN_NUMBER_MAX) * initialBpm.coerceAtLeast(1f))
} else {
    16f * speed.coerceIn(1, 100) * 4f
}

data class PlayerSettings(
    val speed: Int = 1,
    val speedMode: String = PLAYER_SPEED_MODE_FLOATING,
    val greenNumber: Int = PLAYER_GREEN_NUMBER_DEFAULT,
    val keepSpeedAcrossBpm: Boolean = false,
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
    val safeSpeedMode: String get() = speedMode.takeIf {
        it == PLAYER_SPEED_MODE_HI || it == PLAYER_SPEED_MODE_FLOATING
    } ?: PLAYER_SPEED_MODE_FLOATING
    val safeGreenNumber: Int get() = greenNumber.coerceIn(PLAYER_GREEN_NUMBER_MIN, PLAYER_GREEN_NUMBER_MAX)
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
