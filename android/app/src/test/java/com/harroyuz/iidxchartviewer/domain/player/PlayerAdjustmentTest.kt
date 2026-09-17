package com.harroyuz.iidxchartviewer.domain.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerAdjustmentTest {
    @Test fun quarterButtonsRespectOffGridValuesAndMinimum() {
        assertEquals(1.25f, stepHiSpeed(1.12f, true), .0001f)
        assertEquals(1f, stepHiSpeed(1.12f, false), .0001f)
        assertEquals(1.5f, stepHiSpeed(1.25f, true), .0001f)
        assertEquals(1f, stepHiSpeed(1.25f, false), .0001f)
        assertEquals(1f, stepHiSpeed(1f, false), .0001f)
        assertEquals(100f, stepHiSpeed(100f, true), .0001f)
        assertEquals(2.37f, normalizeHiSpeed(2.371f), .0001f)
        assertEquals(1f, normalizeHiSpeed(Float.NaN), .0001f)
    }

    @Test fun whiteNumberUsesThousandthsAndPreservesTailMinimum() {
        for (maximum in listOf(120f, 331.2f, 700f)) {
            val geometry = PlayerFieldGeometry(maximum, 28.8f)
            assertEquals(maximum, geometry.noteHeight(0), .001f)
            assertEquals(maximum / 2, geometry.noteHeight(500), .001f)
            assertTrue(geometry.noteHeight(1000) >= geometry.tailHeight)
            assertTrue(geometry.noteHeight(geometry.maximumWhiteNumber) - geometry.tailHeight < maximum / 1000f + .001f)
            assertTrue(geometry.noteHeight(-1) <= maximum)
        }
    }

    @Test fun floatingTravelTimeRemainsConstantAsFieldHeightChanges() {
        for (height in listOf(28.8f, 150f, 331.2f)) {
            val speed = playerPixelsPerBeat(PLAYER_SPEED_MODE_FLOATING, 2.37f, 300, 150f, height)
            assertEquals(.5f, height / (speed * 150f / 60f), .0001f)
        }
    }

    @Test fun hiSpeedHasFractionalSpacingIndependentOfFieldHeight() {
        assertEquals(151.68f, playerPixelsPerBeat(PLAYER_SPEED_MODE_HI, 2.37f, 300, 150f, 200f), .001f)
    }

    @Test fun clockwiseIncreasesAndCounterclockwiseDecreasesAcrossAngleWrap() {
        val dial = RotaryAdjustment(500f, 0f, 900f)
        dial.move(100f, 0f, 12f, 200f)
        assertEquals(550f, dial.move(0f, 100f, 12f, 200f), .001f)
        assertEquals(600f, dial.move(-100f, 0f, 12f, 200f), .001f)
        assertEquals(650f, dial.move(0f, -100f, 12f, 200f), .001f)
        assertEquals(700f, dial.move(100f, 0f, 12f, 200f), .001f)
        assertEquals(650f, dial.move(0f, -100f, 12f, 200f), .001f)
    }

    @Test fun deadZoneDoesNotJumpAndReversingAtLimitRespondsImmediately() {
        val dial = RotaryAdjustment(890f, 0f, 900f)
        dial.move(100f, 0f, 12f, 200f)
        assertEquals(900f, dial.move(0f, 100f, 12f, 200f), .001f)
        assertEquals(850f, dial.move(100f, 0f, 12f, 200f), .001f)
        assertEquals(850f, dial.move(0f, 0f, 12f, 200f), .001f)
        assertEquals(850f, dial.move(-100f, 0f, 12f, 200f), .001f)
    }
}
