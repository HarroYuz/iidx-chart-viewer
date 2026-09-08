package com.harroyuz.iidxchartviewer.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.BjmMusic
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxAppState
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.score.bjmHistoryRecordKey
import com.harroyuz.iidxchartviewer.domain.score.difficultyIndex
import com.harroyuz.iidxchartviewer.ui.catalog.displayTitle
import com.harroyuz.iidxchartviewer.ui.components.AppTopBar
import com.harroyuz.iidxchartviewer.ui.components.AutoScrollingText
import com.harroyuz.iidxchartviewer.ui.components.StrokedText
import com.harroyuz.iidxchartviewer.ui.components.clearFlagColor
import com.harroyuz.iidxchartviewer.ui.components.clearFlagDetailName
import com.harroyuz.iidxchartviewer.ui.components.difficultyColor
import com.harroyuz.iidxchartviewer.ui.components.difficultyName
import com.harroyuz.iidxchartviewer.ui.player.ChartScoreSummary
import com.harroyuz.iidxchartviewer.ui.theme.Background
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.NormalBlue
import com.harroyuz.iidxchartviewer.ui.theme.Outline
import com.harroyuz.iidxchartviewer.ui.theme.Purple

@Composable
internal fun BjmDataScreen(
    state: IidxAppState,
    history: List<BjmScore>,
    bjmIndex: BjmIndex,
    onOpenMenu: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenSong: (IidxChart) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedDate: String?,
    onSelectedDateChange: (String?) -> Unit,
    calendarExpanded: Boolean,
    onCalendarExpandedChange: (Boolean) -> Unit,
    calendarMonth: String,
    onCalendarMonthChange: (String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    var logoutConfirmationVisible by remember { mutableStateOf(false) }
    val chartsById = remember(state.charts) { state.charts.associateBy { it.id } }
    val historyCharts = remember(state.songGroups, state.charts, bjmIndex.songMusicIds) {
        buildBjmHistoryChartIndex(state, bjmIndex, chartsById)
    }
    val musicById = remember(state.bjmMusic) { state.bjmMusic.associateBy { it.musicId } }
    val historyCountByDate = remember(history) {
        history.groupingBy { record -> bjmHistoryRecordDate(record.time) }.eachCount()
    }
    val filteredHistory = remember(history, query, selectedDate, historyCharts, musicById) {
        val normalizedQuery = query.trim()
        history.filter { record ->
            val chart = historyCharts[record.key]
            val music = musicById[record.musicId]
            val matchesQuery = normalizedQuery.isBlank() || listOf(
                chart?.title,
                chart?.subtitle,
                chart?.genre,
                chart?.composer,
                music?.title,
                music?.plainTitle,
                music?.artist,
            ).filterNotNull().joinToString(" ").contains(normalizedQuery, ignoreCase = true)
            val matchesDate = selectedDate == null || bjmHistoryRecordDate(record.time) == selectedDate
            matchesQuery && matchesDate
        }
    }
    LaunchedEffect(query, selectedDate) {
        listState.scrollToItem(0)
    }

    Column(modifier.fillMaxSize().background(Background)) {
        AppTopBar(title = "成绩历史", onNavigate = onOpenMenu, menu = true)
        HorizontalDivider(color = Outline)
        if (state.bjmUser == null) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("请先登录 BJM", color = Ink, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = onLogin) {
                    Text("登录 BJM", color = Purple, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${state.bjmUser.name.ifBlank { state.bjmUser.id }} · ${history.size} 条历史记录",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { logoutConfirmationVisible = true },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text("登出", color = Purple, fontSize = 12.sp)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("搜索曲目", color = Muted) },
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Text("×", color = Muted, fontSize = 20.sp)
                                }
                            }
                        },
                    )
                    IconButton(onClick = { onCalendarExpandedChange(!calendarExpanded) }) {
                        CalendarIcon(if (calendarExpanded || selectedDate != null) Purple else Muted)
                    }
                }
                if (calendarExpanded) {
                    HistoryCalendar(
                        month = calendarMonth,
                        selectedDate = selectedDate,
                        recordCounts = historyCountByDate,
                        onMonthChange = onCalendarMonthChange,
                        onDateSelected = { date ->
                            onSelectedDateChange(date)
                            date?.let { onCalendarMonthChange(it.substring(0, 7)) }
                        },
                    )
                } else if (selectedDate != null) {
                    Text(
                        "已筛选：$selectedDate",
                        color = Muted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 1.dp),
                    )
                }
                if (filteredHistory.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            if (history.isEmpty()) "暂无历史成绩" else "没有匹配的历史记录",
                            color = Muted,
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().weight(1f),
                        state = listState,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp),
                    ) {
                        items(filteredHistory, key = ::bjmHistoryRecordKey) { record ->
                            BjmHistoryRow(
                                record = record,
                                chart = historyCharts[record.key],
                                music = musicById[record.musicId],
                                onOpenSong = onOpenSong,
                            )
                        }
                    }
                }
            }
        }
    }
    if (logoutConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { logoutConfirmationVisible = false },
            title = { Text("退出 BJM") },
            text = { Text("确定要退出当前 BJM 账号吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        logoutConfirmationVisible = false
                        onLogout()
                    },
                ) { Text("退出", color = Purple) }
            },
            dismissButton = {
                TextButton(onClick = { logoutConfirmationVisible = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun BjmHistoryRow(
    record: BjmScore,
    chart: IidxChart?,
    music: BjmMusic?,
    onOpenSong: (IidxChart) -> Unit,
) {
    val title = chart?.let { displayTitle(it.title, it.sourceLabel) }
        ?: music?.title?.takeIf { it.isNotBlank() }
        ?: "未知曲目"
    val subtitle = chart?.subtitle?.takeIf { it.isNotBlank() }
    val difficulty = chart?.let { "${it.mode} ${difficultyName(it.difficulty)} ${it.level}" }
        ?: "${if (record.playStyle == 1) "DP" else "SP"} ${difficultyName(difficultyCode(record.noteId))}"
    val rowHeight = if (subtitle != null) 72.dp else 62.dp
    Column(
        Modifier.fillMaxWidth()
            .height(rowHeight)
            .clickable(enabled = chart != null) { chart?.let(onOpenSong) }
            .padding(horizontal = 20.dp, vertical = 5.dp),
    ) {
        Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = if (subtitle == null) Arrangement.SpaceBetween else Arrangement.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    AutoScrollingText(title, color = Ink, fontSize = 14.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
                    subtitle?.let {
                        AutoScrollingText(it, color = Muted, fontSize = 10.sp, lineHeight = 11.sp)
                    }
                }
                if (subtitle != null) Spacer(Modifier.weight(1f))
                Text(
                    difficulty,
                    color = difficultyColor(chart?.difficulty ?: difficultyCode(record.noteId)),
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(formatBjmHistoryTime(record.time), color = Muted, fontSize = 10.sp, lineHeight = 11.sp)
            }
            if (chart != null) {
                ChartScoreSummary(
                    score = record,
                    noteCount = chart.notes,
                )
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    StrokedText(
                        text = clearFlagDetailName(record.clearFlag),
                        fillColor = clearFlagColor(record.clearFlag),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(record.exScore.toString(), color = NormalBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        HorizontalDivider(color = Outline)
    }
}

private fun buildBjmHistoryChartIndex(
    state: IidxAppState,
    bjmIndex: BjmIndex,
    chartsById: Map<String, IidxChart>,
): Map<String, IidxChart> = buildMap {
    state.songGroups.forEach { group ->
        val musicId = bjmIndex.songMusicIds[group.key] ?: return@forEach
        group.chartIds
            .mapNotNull { chartsById[it] }
            .groupBy { chart ->
                "$musicId:${if (chart.mode == "DP") 1 else 0}:${difficultyIndex(chart.difficulty)}"
            }
            .forEach { (key, candidates) ->
                val chart = candidates.maxWithOrNull(
                    compareBy<IidxChart>({ it.textageUrl != null }, { it.notes }, { it.bpm.isNotBlank() }),
                ) ?: return@forEach
                put(key, chart)
            }
    }
}
