package com.harroyuz.iidxchartviewer.domain.player

import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

internal fun normalizeHiSpeed(value: Float): Float =
    if (value.isFinite()) (value.coerceIn(1f, 100f) * 100).roundToInt() / 100f else 1f

/** Move to the next quarter in the requested direction, including off-grid input. */
internal fun stepHiSpeed(value: Float, increase: Boolean): Float {
    val hundredths = (normalizeHiSpeed(value) * 100).roundToInt()
    val next = if (increase) (hundredths / 25 + 1) * 25 else ((hundredths - 1) / 25) * 25
    return next.coerceIn(100, 10000) / 100f
}

/** White number follows SUDDEN+: 100 units cover 10% of the full note field. */
internal data class PlayerFieldGeometry(val maximumNoteHeight: Float, val tailHeight: Float) {
    val maximumWhiteNumber: Int
        get() = floor(1000f * (1f - tailHeight / maximumNoteHeight.coerceAtLeast(tailHeight)))
            .toInt().coerceIn(0, 999)

    fun noteHeight(whiteNumber: Int): Float =
        (maximumNoteHeight * (1f - whiteNumber.coerceIn(0, maximumWhiteNumber) / 1000f))
            .coerceAtLeast(tailHeight)
}

/** Screen coordinates have a downward Y axis, so positive angles turn clockwise. */
internal class RotaryAdjustment(initial: Float, private val minimum: Float, private val maximum: Float) {
    var value: Float = initial.coerceIn(minimum, maximum)
        private set
    private var lastAngle: Float? = null

    fun move(x: Float, y: Float, deadZone: Float, unitsPerTurn: Float): Float {
        if (hypot(x, y) < deadZone) {
            lastAngle = null
            return value
        }
        val angle = Math.toDegrees(atan2(y, x).toDouble()).toFloat()
        lastAngle?.let { previous ->
            val delta = (angle - previous + 540f) % 360f - 180f
            value = (value + delta * unitsPerTurn / 360f).coerceIn(minimum, maximum)
        }
        lastAngle = angle
        return value
    }
}
