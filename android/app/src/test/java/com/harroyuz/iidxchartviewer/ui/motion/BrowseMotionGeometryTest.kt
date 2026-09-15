package com.harroyuz.iidxchartviewer.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowseMotionGeometryTest {
    @Test fun shortPanelStartsAtViewportBottomRatherThanOnePanelHeightAway() {
        val top = 700f
        assertEquals(1400f, top + bottomRevealDistance(1400f, top), .01f)
    }

    @Test fun playerAndRadarShareTheSameOffscreenBoundaryAtDifferentPositions() {
        for (top in listOf(240f, 800f, 1200f)) {
            assertEquals(1600f, top + bottomRevealDistance(1600f, top), .01f)
        }
    }

    @Test fun alreadyOffscreenPanelDoesNotMoveBackIntoView() {
        assertEquals(0f, bottomRevealDistance(1000f, 1200f), .01f)
    }

    @Test fun neighboringCardsStartOutsideOppositeViewportEdges() {
        val top = 450f
        val height = 180f
        assertEquals(0f, top + height + catalogRevealDistance(1400f, top, height, true), .01f)
        assertEquals(1400f, top + catalogRevealDistance(1400f, top, height, false), .01f)
    }
}
