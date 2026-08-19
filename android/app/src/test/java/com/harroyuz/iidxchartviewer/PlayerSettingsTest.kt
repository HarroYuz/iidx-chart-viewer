package com.harroyuz.iidxchartviewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSettingsTest {
    @Test
    fun floatingHiSpeedUsesGreenNumberTravelTimeAtStartingBpm() {
        val greenNumber = 300
        val initialBpm = 150f
        val judgeDistance = 331.2f
        val pixelsPerBeat = playerPixelsPerBeat(
            speedMode = PLAYER_SPEED_MODE_FLOATING,
            speed = 1,
            greenNumber = greenNumber,
            initialBpm = initialBpm,
            judgeDistancePx = judgeDistance,
        )

        val travelSeconds = judgeDistance / (pixelsPerBeat * initialBpm / 60f)
        assertEquals(greenNumber / 600f, travelSeconds, 0.0001f)
    }

    @Test
    fun hiSpeedKeepsTheExistingFourTimesBaseline() {
        assertEquals(
            64f,
            playerPixelsPerBeat(
                speedMode = PLAYER_SPEED_MODE_HI,
                speed = 1,
                greenNumber = PLAYER_GREEN_NUMBER_DEFAULT,
                initialBpm = 150f,
                judgeDistancePx = 331.2f,
            ),
            0.0001f,
        )
        assertTrue(PlayerSettings().safeSpeedMode == PLAYER_SPEED_MODE_FLOATING)
        assertEquals(PLAYER_GREEN_NUMBER_DEFAULT, PlayerSettings().safeGreenNumber)
        assertEquals(500, PLAYER_GREEN_NUMBER_DEFAULT)
        assertFalse(PlayerSettings().keepSpeedAcrossBpm)
    }
}
