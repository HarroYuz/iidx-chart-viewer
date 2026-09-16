package com.harroyuz.iidxchartviewer.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.BjmMusic
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxAppState
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.domain.catalog.buildCatalogVersionOptions
import com.harroyuz.iidxchartviewer.domain.catalog.catalogSongTypes
import com.harroyuz.iidxchartviewer.domain.catalog.toggleCatalogSongType
import com.harroyuz.iidxchartviewer.domain.score.matchesHistoryCatalogFilters
import com.harroyuz.iidxchartviewer.domain.score.bjmHistoryRecordKey
import com.harroyuz.iidxchartviewer.ui.catalog.displayTitle
import com.harroyuz.iidxchartviewer.ui.catalog.CatalogFilterPanel
import com.harroyuz.iidxchartviewer.ui.catalog.FunnelIcon
import com.harroyuz.iidxchartviewer.ui.catalog.filterLabel
import com.harroyuz.iidxchartviewer.ui.components.AppTopBar
import com.harroyuz.iidxchartviewer.ui.components.AutoScrollingText
import com.harroyuz.iidxchartviewer.ui.components.StrokedText
import com.harroyuz.iidxchartviewer.ui.components.clearFlagColor
import com.harroyuz.iidxchartviewer.ui.components.clearFlagDetailName
import com.harroyuz.iidxchartviewer.ui.components.difficultyColor
import com.harroyuz.iidxchartviewer.ui.components.difficultyName
import com.harroyuz.iidxchartviewer.domain.score.resolveBjmHistoryTarget
import com.harroyuz.iidxchartviewer.domain.score.BjmHistoryTarget
import com.harroyuz.iidxchartviewer.domain.score.buildBjmHistoryChartIndex
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
    visualEffectsDisabled: Boolean,
    calendarMonth: String,
    onCalendarMonthChange: (String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    var logoutConfirmationVisible by remember { mutableStateOf(false) }
    var filterExpanded by rememberSaveable { mutableStateOf(false) }
    var searchFields by rememberSaveable { mutableStateOf(listOf("曲名", "曲师", "曲风")) }
    var selectedVersions by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedLevels by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var selectedTypeNames by rememberSaveable { mutableStateOf(catalogSongTypes.map { it.name }) }
    val selectedTypes = selectedTypeNames.map(ArcadeStatus::valueOf).toSet()
    val orderedSearchFields = listOf("曲名", "曲师", "曲风").filter { it in searchFields }
    val searchPlaceholder = when (orderedSearchFields.size) {
        1 -> "搜索${orderedSearchFields.first()}"
        2 -> "搜索${orderedSearchFields.joinToString("或")}"
        else -> "搜索${orderedSearchFields[0]}、${orderedSearchFields[1]}或${orderedSearchFields[2]}"
    }
    val versionOptions = remember(state.charts) { buildCatalogVersionOptions(state.charts) }
    fun resetFilters() {
        searchFields = listOf("曲名", "曲师", "曲风")
        selectedVersions = emptyList()
        selectedLevels = emptyList()
        selectedTypeNames = catalogSongTypes.map { it.name }
        onSelectedDateChange(null)
    }
    val historyCharts = remember(state.songGroups, state.charts, bjmIndex.songMusicIds) {
        buildBjmHistoryChartIndex(state, bjmIndex)
    }
    val musicById = remember(state.bjmMusic) { state.bjmMusic.associateBy { it.musicId } }
    val recordDates = remember(history) { history.map { it.time }.distinct().associateWith(::bjmHistoryRecordDate) }
    val matchingHistory = remember(history, query, searchFields, selectedVersions, selectedLevels, selectedTypes, historyCharts, musicById) {
        val fields = searchFields.toSet()
        val versions = selectedVersions.toSet()
        val levels = selectedLevels.toSet()
        history.filter { record ->
            val chart = resolveBjmHistoryTarget(record, historyCharts[record.key].orEmpty()).navigationChart
            matchesHistoryCatalogFilters(chart, musicById[record.musicId], query, fields, versions, levels, selectedTypes)
        }
    }
    val historyCountByDate = remember(matchingHistory) {
        matchingHistory.groupingBy { record -> recordDates.getValue(record.time) }.eachCount()
    }
    val filteredHistory = remember(matchingHistory, selectedDate) {
        matchingHistory.filter { selectedDate == null || recordDates.getValue(it.time) == selectedDate }
    }
    val activeSummary = buildList {
        if (searchFields.size < 3) add("仅搜索${orderedSearchFields.joinToString("/")}")
        if (selectedTypes.size < catalogSongTypes.size) add("曲目类型：${catalogSongTypes.filter { it in selectedTypes }.joinToString("/") { it.filterLabel() }}")
        if (selectedLevels.isNotEmpty()) add("LEVEL ${selectedLevels.sorted().joinToString("/")}")
        if (selectedVersions.isNotEmpty()) add("版本 ${versionOptions.filter { it.value in selectedVersions }.joinToString("/") { it.abbreviation }}")
        selectedDate?.let { add("日期 $it") }
    }.joinToString("，")
    LaunchedEffect(query, selectedDate, searchFields, selectedVersions, selectedLevels, selectedTypes) {
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
                        placeholder = { Text(searchPlaceholder, color = Muted) },
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
                    IconButton(onClick = { filterExpanded = !filterExpanded }, modifier = Modifier.semantics {
                        contentDescription = if (filterExpanded) "收起历史筛选" else "展开历史筛选"
                    }) {
                        FunnelIcon(if (filterExpanded || activeSummary.isNotBlank()) Purple else Muted)
                    }
                }
                AnimatedVisibility(
                    visible = filterExpanded,
                    enter = if (visualEffectsDisabled) EnterTransition.None else
                        expandVertically(tween(180), expandFrom = Alignment.Top) + fadeIn(tween(120)),
                    exit = if (visualEffectsDisabled) ExitTransition.None else
                        shrinkVertically(tween(180), shrinkTowards = Alignment.Top) + fadeOut(tween(100)),
                ) {
                    CatalogFilterPanel(
                        searchFields = orderedSearchFields,
                        onSearchFieldToggle = { field ->
                            if (field !in searchFields) searchFields = searchFields + field
                            else if (searchFields.size > 1) searchFields = searchFields - field
                        },
                        types = selectedTypes,
                        onTypeToggle = { selectedTypeNames = toggleCatalogSongType(selectedTypes, it).map { type -> type.name } },
                        levels = selectedLevels.toSet(),
                        onLevelToggle = { selectedLevels = if (it in selectedLevels) selectedLevels - it else selectedLevels + it },
                        versions = selectedVersions.toSet(),
                        versionOptions = versionOptions,
                        onVersionToggle = { selectedVersions = if (it in selectedVersions) selectedVersions - it else selectedVersions + it },
                        onClearLevels = { selectedLevels = emptyList() },
                        onClearVersions = { selectedVersions = emptyList() },
                        onReset = ::resetFilters,
                        visualEffectsDisabled = visualEffectsDisabled,
                        dateSummary = selectedDate?.substringAfter('-') ?: "全部",
                        dateContent = {
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
                        },
                    )
                }
                if (!filterExpanded && activeSummary.isNotBlank()) {
                    Text(
                        activeSummary,
                        color = Muted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 1.dp),
                    )
                }
                Text("${filteredHistory.size} 条记录", color = Muted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                if (filteredHistory.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (history.isEmpty()) "暂无历史成绩" else "没有匹配的历史记录", color = Muted, fontSize = 14.sp)
                            if (query.isNotBlank() || activeSummary.isNotBlank()) TextButton(onClick = {
                                onQueryChange("")
                                resetFilters()
                            }) { Text("清除筛选") }
                        }
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
                                target = resolveBjmHistoryTarget(record, historyCharts[record.key].orEmpty()),
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
    target: BjmHistoryTarget,
    music: BjmMusic?,
    onOpenSong: (IidxChart) -> Unit,
) {
    val chart = target.compatibleChart
    val song = target.navigationChart
    val title = song?.let { displayTitle(it.title, it.sourceLabel) }
        ?: music?.title?.takeIf { it.isNotBlank() }
        ?: "未知曲目"
    val subtitle = song?.subtitle?.takeIf { it.isNotBlank() }
    val difficulty = chart?.let { "${it.mode} ${difficultyName(it.difficulty)} ${it.level}" }
        ?: "${if (record.playStyle == 1) "DP" else "SP"} ${difficultyName(difficultyCode(record.noteId))}"
    val rowHeight = if (subtitle != null) 72.dp else 62.dp
    Column(
        Modifier.fillMaxWidth()
            .height(rowHeight)
            .clickable(enabled = song != null) { song?.let(onOpenSong) }
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
