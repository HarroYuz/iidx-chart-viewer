package com.harroyuz.iidxchartviewer.domain.sync

import java.util.Calendar

internal const val UPDATE_CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L

internal fun isSameLocalDate(timestamp: Long, now: Long = System.currentTimeMillis()): Boolean {
    if (timestamp <= 0L) return false
    val timestampCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }
    return timestampCalendar.get(Calendar.ERA) == nowCalendar.get(Calendar.ERA) &&
        timestampCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
        timestampCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR)
}
