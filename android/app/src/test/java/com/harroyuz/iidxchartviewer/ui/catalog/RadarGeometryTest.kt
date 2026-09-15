package com.harroyuz.iidxchartviewer.ui.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class RadarGeometryTest {
    @Test fun hundredIsTheGridAndTwoHundredExtendsToDoubleItsRadius() {
        assertEquals(1f, 100f / RADAR_GRID_MAX, 0f)
        assertEquals(1.5f, 150f / RADAR_GRID_MAX, 0f)
        assertEquals(2f, RADAR_VALUE_MAX / RADAR_GRID_MAX, 0f)
    }

    @Test fun maximumVerticesAndLabelsStayInsideThePlotAcrossSizesAndFontScales() {
        for ((width, height) in listOf(180f to 188f, 252f to 188f, 340f to 220f)) {
            for (fontScale in listOf(1f, 1.3f, 2f)) {
                val labelWidth = 44f * fontScale
                val labelHeight = 12f * fontScale
                val geometry = radarGeometry(width, height, labelWidth, labelHeight, 8f, 2.5f)
                val fullRadius = geometry.gridRadius * 200f / RADAR_GRID_MAX
                for (index in 0..5) {
                    val angle = -PI / 2 + index * PI / 3
                    val x = cos(angle).toFloat()
                    val y = sin(angle).toFloat()
                    assertTrue(abs(x * fullRadius) + 2.5f <= width / 2f - 8f)
                    assertTrue(abs(y * fullRadius) + 2.5f <= height / 2f - 8f)
                    assertTrue(abs(x * geometry.labelRadius) + labelWidth / 2f <= width / 2f - 8f + .001f)
                    assertTrue(abs(y * geometry.labelRadius) + labelHeight / 2f <= height / 2f - 8f + .001f)
                }
            }
        }
    }

    @Test fun labelsCanSitInsideTheOverflowWithoutShrinkingTheGrid() {
        val normal = radarGeometry(252f, 188f, 44f, 12f, 8f, 2.5f)
        val largerFont = radarGeometry(252f, 188f, 88f, 24f, 8f, 2.5f)
        assertEquals(normal.gridRadius, largerFont.gridRadius, 0f)
        assertTrue(normal.labelRadius > normal.gridRadius)
        assertTrue(normal.labelRadius < normal.gridRadius * 2f)
    }

    @Test fun constrainedPlotNeverProducesANegativeRadius() {
        val geometry = radarGeometry(30f, 20f, 44f, 24f, 8f, 2.5f)
        assertEquals(0f, geometry.gridRadius, 0f)
        assertEquals(0f, geometry.labelRadius, 0f)
    }
}
