package com.harroyuz.iidxchartviewer.ui.history

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun difficultyCode(noteId: Int): String = when (noteId) {
    0 -> "B"
    1 -> "N"
    2 -> "H"
    3 -> "A"
    4 -> "L"
    else -> "?"
}

internal fun formatBjmHistoryTime(value: Long): String {
    if (value <= 0L) return "时间未知"
    val millis = when {
        value < 10_000_000_000L -> value * 1_000L
        value > 100_000_000_000_000L -> value / 1_000L
        else -> value
    }
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
    }.getOrElse { "时间 $value" }
}
