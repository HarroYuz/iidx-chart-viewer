package com.harroyuz.iidxchartviewer.domain.player

import org.junit.Assert.*
import org.junit.Test

class ChartPrefetchPolicyTest {
    private val minimum = 192L * 1024 * 1024

    @Test fun lowRamDevicesNeverPrefetchEvenWhenCurrentlyIdle() {
        assertFalse(canPrefetchCharts(true, false, 256, 2 * minimum))
    }

    @Test fun memoryPressureAndSmallHeapEachPreventPrefetch() {
        assertFalse(canPrefetchCharts(false, true, 256, 2 * minimum))
        assertFalse(canPrefetchCharts(false, false, 127, 2 * minimum))
        assertFalse(canPrefetchCharts(false, false, 256, minimum - 1))
    }

    @Test fun sufficientHeadroomAllowsBoundedOptionalWork() {
        assertTrue(canPrefetchCharts(false, false, 128, minimum))
    }
}
