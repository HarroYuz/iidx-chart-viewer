package com.harroyuz.iidxchartviewer.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.NormalBlue
import com.harroyuz.iidxchartviewer.ui.theme.Outline
import com.harroyuz.iidxchartviewer.ui.theme.Purple
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun CalendarIcon(color: ComposeColor) {
    Canvas(Modifier.size(22.dp)) {
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * .14f, size.height * .20f),
            size = Size(size.width * .72f, size.height * .66f),
            cornerRadius = CornerRadius(size.width * .08f),
            style = Stroke(width = 2f),
        )
        drawLine(
            color,
            Offset(size.width * .14f, size.height * .40f),
            Offset(size.width * .86f, size.height * .40f),
            strokeWidth = 2f,
        )
        drawLine(
            color,
            Offset(size.width * .32f, size.height * .10f),
            Offset(size.width * .32f, size.height * .28f),
            strokeWidth = 2f,
        )
        drawLine(
            color,
            Offset(size.width * .68f, size.height * .10f),
            Offset(size.width * .68f, size.height * .28f),
            strokeWidth = 2f,
        )
    }
}

@Composable
internal fun HistoryCalendar(
    month: String,
    selectedDate: String?,
    recordCounts: Map<String, Int>,
    onMonthChange: (String) -> Unit,
    onDateSelected: (String?) -> Unit,
) {
    val displayCalendar = remember(month) { historyMonthCalendar(month) }
    var pickerMode by remember(month) { mutableStateOf<HistoryPickerMode?>(null) }
    var pickerYear by remember(month) { mutableStateOf(displayCalendar.get(Calendar.YEAR)) }
    val displayYear = displayCalendar.get(Calendar.YEAR)
    val displayMonth = displayCalendar.get(Calendar.MONTH) + 1
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp)
            .border(1.dp, Outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { onMonthChange(shiftHistoryMonth(month, -1)) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            ) { Text("‹", color = Purple, fontSize = 20.sp) }
            Box(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            pickerYear = displayYear
                            pickerMode = if (pickerMode == HistoryPickerMode.YEAR) null else HistoryPickerMode.YEAR
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    ) {
                        Text("${displayYear}年", color = if (pickerMode == HistoryPickerMode.YEAR) Purple else Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("/", color = Muted, fontSize = 12.sp)
                    TextButton(
                        onClick = {
                            pickerYear = displayYear
                            pickerMode = if (pickerMode == HistoryPickerMode.MONTH) null else HistoryPickerMode.MONTH
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    ) {
                        Text("${displayMonth}月", color = if (pickerMode == HistoryPickerMode.MONTH) Purple else Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (selectedDate != null) {
                    TextButton(
                        onClick = { onDateSelected(null) },
                        modifier = Modifier.align(Alignment.CenterEnd),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) { Text("清除", color = Muted, fontSize = 10.sp) }
                }
            }
            TextButton(
                onClick = { onMonthChange(shiftHistoryMonth(month, 1)) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            ) { Text("›", color = Purple, fontSize = 20.sp) }
        }
        if (pickerMode != null) {
            HistoryMonthPicker(
                mode = pickerMode!!,
                selectedYear = pickerYear,
                recordCounts = recordCounts,
                currentYear = displayYear,
                currentMonth = displayMonth,
                onYearSelected = { pickerYear = it; pickerMode = HistoryPickerMode.MONTH },
                onMonthSelected = { selectedMonth ->
                    onMonthChange("%04d-%02d".format(Locale.ROOT, pickerYear, selectedMonth))
                    pickerMode = null
                },
                onDismiss = { pickerMode = null },
            )
        } else {
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Text(
                        day,
                        color = Muted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            historyCalendarDays(month).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(
                            Modifier.weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (date.value == selectedDate) Purple.copy(alpha = .16f) else ComposeColor.Transparent)
                                .clickable { onDateSelected(date.value) },
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                color = when {
                                    date.value == selectedDate -> Purple
                                    !date.inCurrentMonth -> Muted
                                    else -> Ink
                                },
                                fontSize = 11.sp,
                                fontWeight = if (date.inCurrentMonth || date.value == selectedDate) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 9.dp),
                            )
                            recordCounts[date.value]?.takeIf { it > 0 }?.let { count ->
                                Text(
                                    "${count}项",
                                    color = NormalBlue,
                                    fontSize = 8.sp,
                                    lineHeight = 8.sp,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class HistoryPickerMode {
    YEAR,
    MONTH,
}

@Composable
private fun HistoryMonthPicker(
    mode: HistoryPickerMode,
    selectedYear: Int,
    recordCounts: Map<String, Int>,
    currentYear: Int,
    currentMonth: Int,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .heightIn(max = 280.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = onDismiss,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            ) { Text("返回日历", color = Muted, fontSize = 10.sp) }
        }
        if (mode == HistoryPickerMode.YEAR) {
            Text("选择年份", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            historyCalendarYears(recordCounts, selectedYear).chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { year ->
                        HistoryPickerCell(
                            label = "${year}年",
                            count = historyRecordCountForYear(recordCounts, year),
                            selected = year == selectedYear,
                            modifier = Modifier.weight(1f),
                            onClick = { onYearSelected(year) },
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        } else {
            Text("选择月份（${selectedYear}年）", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            (1..12).toList().chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { month ->
                        HistoryPickerCell(
                            label = "${month}月",
                            count = historyRecordCountForMonth(recordCounts, selectedYear, month),
                            selected = selectedYear == currentYear && month == currentMonth,
                            modifier = Modifier.weight(1f),
                            onClick = { onMonthSelected(month) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryPickerCell(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .padding(2.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Purple.copy(alpha = .14f) else ComposeColor.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = if (selected) Purple else Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("${count}项", color = if (count > 0) Purple else Muted, fontSize = 8.sp, lineHeight = 8.sp)
    }
}

private fun historyCalendarYears(recordCounts: Map<String, Int>, currentYear: Int): List<Int> {
    val years = recordCounts.keys.mapNotNull { it.substringBefore('-').toIntOrNull() } + currentYear
    val minYear = years.minOrNull() ?: currentYear
    val maxYear = years.maxOrNull() ?: currentYear
    return (minYear..maxYear).toList()
}

private fun historyRecordCountForYear(recordCounts: Map<String, Int>, year: Int): Int {
    val prefix = "%04d-".format(Locale.ROOT, year)
    return recordCounts.filterKeys { it.startsWith(prefix) }.values.sum()
}

private fun historyRecordCountForMonth(recordCounts: Map<String, Int>, year: Int, month: Int): Int {
    val prefix = "%04d-%02d-".format(Locale.ROOT, year, month)
    return recordCounts.filterKeys { it.startsWith(prefix) }.values.sum()
}

private fun historyTimeMillis(value: Long): Long = when {
    value < 10_000_000_000L -> value * 1_000L
    value > 100_000_000_000_000L -> value / 1_000L
    else -> value
}

internal fun bjmHistoryRecordDate(value: Long): String =
    if (value <= 0L) "" else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(historyTimeMillis(value)))

internal fun historyMonthKey(value: Long): String =
    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(historyTimeMillis(value)))

private fun historyMonthCalendar(value: String): Calendar = Calendar.getInstance().apply {
    val parts = value.split('-')
    set(Calendar.YEAR, parts.getOrNull(0)?.toIntOrNull() ?: get(Calendar.YEAR))
    set(Calendar.MONTH, (parts.getOrNull(1)?.toIntOrNull() ?: (get(Calendar.MONTH) + 1)) - 1)
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun shiftHistoryMonth(value: String, offset: Int): String = historyMonthCalendar(value).apply {
    add(Calendar.MONTH, offset)
}.let(::historyMonthKeyFromCalendar)

private fun historyMonthKeyFromCalendar(calendar: Calendar): String =
    "%04d-%02d".format(Locale.ROOT, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)

private data class HistoryCalendarDay(
    val value: String,
    val dayOfMonth: Int,
    val inCurrentMonth: Boolean,
)

private fun historyCalendarDays(value: String): List<HistoryCalendarDay> {
    val calendar = historyMonthCalendar(value)
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val firstDayOffset = (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cellCount = ((firstDayOffset + daysInMonth + 6) / 7) * 7
    val firstGridDay = (calendar.clone() as Calendar).apply {
        add(Calendar.DAY_OF_MONTH, -firstDayOffset)
    }
    return List(cellCount) { index ->
        val day = (firstGridDay.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, index)
        }
        HistoryCalendarDay(
            value = "%04d-%02d-%02d".format(
                Locale.ROOT,
                day.get(Calendar.YEAR),
                day.get(Calendar.MONTH) + 1,
                day.get(Calendar.DAY_OF_MONTH),
            ),
            dayOfMonth = day.get(Calendar.DAY_OF_MONTH),
            inCurrentMonth = day.get(Calendar.YEAR) == year && day.get(Calendar.MONTH) == month,
        )
    }
}
