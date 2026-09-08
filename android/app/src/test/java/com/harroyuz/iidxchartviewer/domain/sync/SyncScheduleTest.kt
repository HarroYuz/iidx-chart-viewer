package com.harroyuz.iidxchartviewer.domain.sync

import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test

class SyncScheduleTest {
    private fun timestamp(year: Int, day: Int, hour: Int) = Calendar.getInstance().apply {
        clear()
        set(year, Calendar.JANUARY, day, hour, 0, 0)
    }.timeInMillis

    @Test fun comparesCalendarDayRatherThanElapsedHours() {
        assertTrue(isSameLocalDate(timestamp(2026, 1, 0), timestamp(2026, 1, 23)))
        assertFalse(isSameLocalDate(timestamp(2026, 1, 23), timestamp(2026, 2, 0)))
        assertFalse(isSameLocalDate(timestamp(2025, 1, 0), timestamp(2026, 1, 0)))
        assertFalse(isSameLocalDate(0, timestamp(2026, 1, 0)))
    }
}
