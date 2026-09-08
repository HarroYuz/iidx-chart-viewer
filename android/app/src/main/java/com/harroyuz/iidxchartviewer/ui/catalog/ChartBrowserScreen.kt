package com.harroyuz.iidxchartviewer.ui.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import com.harroyuz.iidxchartviewer.ui.components.AppTopBar
import com.harroyuz.iidxchartviewer.ui.components.PlayStyleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.BuildConfig
import com.harroyuz.iidxchartviewer.R
import com.harroyuz.iidxchartviewer.domain.catalog.difficultyOrder
import com.harroyuz.iidxchartviewer.domain.catalog.textageVersionIndex
import com.harroyuz.iidxchartviewer.domain.catalog.versionNumber
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxAppState
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.model.TextageSyncProgress
import com.harroyuz.iidxchartviewer.domain.sync.DataSyncTarget
import com.harroyuz.iidxchartviewer.ui.components.TextageSyncBanner
import com.harroyuz.iidxchartviewer.ui.history.BjmDataScreen
import com.harroyuz.iidxchartviewer.ui.history.historyMonthKey
import com.harroyuz.iidxchartviewer.ui.settings.UpdateSettingsScreen
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.Panel
import com.harroyuz.iidxchartviewer.ui.theme.Purple
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
internal fun ChartBrowserScreen(
    state: IidxAppState,
    bjmHistory: List<BjmScore>,
    bjmIndex: BjmIndex,
    mode: String,
    onModeChange: (String) -> Unit,
    textageSyncing: Boolean,
    textageProgress: TextageSyncProgress?,
    textageError: String?,
    textageLastSyncAt: Long,
    bjmMusicLastSyncAt: Long,
    bjmScoresLastSyncAt: Long,
    onLogin: () -> Unit,
    onOpenBjmData: () -> Unit,
    onRefreshTextage: () -> Unit,
    onFullDataSync: () -> Unit,
    onSyncTextage: () -> Unit,
    onSyncBjmMusic: () -> Unit,
    onSyncBjmScores: () -> Unit,
    onOpenGithub: () -> Unit,
    onCheckForUpdates: () -> Unit,
    updateChecking: Boolean,
    syncTarget: DataSyncTarget?,
    syncStage: String?,
    syncProgress: Float?,
    settingsPageVisible: Boolean,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    bjmDataPageVisible: Boolean,
    onDismissBjmData: () -> Unit,
    onOpenSongFromBjmHistory: (IidxChart) -> Unit,
    onLogoutBjm: () -> Unit,
    autoUpdateEnabled: Boolean,
    onAutoUpdateEnabledChange: (Boolean) -> Unit,
    onClearChartCache: () -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    onOpenSong: (IidxChart) -> Unit,
    onCopyText: (String) -> Unit,
    showingDetail: Boolean,
    onBack: () -> Unit,
    onRequestExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedVersion by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLevel by rememberSaveable { mutableStateOf<Int?>(null) }
    var searchGenreEnabled by rememberSaveable { mutableStateOf(true) }
    var searchTitleEnabled by rememberSaveable { mutableStateOf(true) }
    var searchComposerEnabled by rememberSaveable { mutableStateOf(true) }
    var bjmHistoryQuery by rememberSaveable { mutableStateOf("") }
    var bjmHistoryDate by rememberSaveable { mutableStateOf<String?>(null) }
    var bjmHistoryCalendarExpanded by rememberSaveable { mutableStateOf(false) }
    var bjmHistoryCalendarMonth by rememberSaveable { mutableStateOf(historyMonthKey(System.currentTimeMillis())) }
    val bjmHistoryListState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    fun closeDrawer() {
        drawerScope.launch { drawerState.close() }
    }

    BackHandler {
        when {
            drawerState.isOpen -> closeDrawer()
            showingDetail -> onBack()
            settingsPageVisible -> onDismissSettings()
            bjmDataPageVisible -> onRequestExit()
            else -> onRequestExit()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !showingDetail,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(180.dp),
                drawerShape = RoundedCornerShape(0.dp),
            ) {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        "菜单",
                        color = Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
                    )
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NavigationDrawerItem(
                            label = { Text("曲库") },
                            selected = !settingsPageVisible && !bjmDataPageVisible,
                            onClick = {
                                closeDrawer()
                                onDismissSettings()
                                onDismissBjmData()
                            },
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        NavigationDrawerItem(
                            label = { Text("成绩历史") },
                            selected = bjmDataPageVisible,
                            onClick = {
                                closeDrawer()
                                onDismissSettings()
                                onOpenBjmData()
                            },
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        NavigationDrawerItem(
                            label = { Text("设置") },
                            selected = settingsPageVisible,
                            onClick = {
                                closeDrawer()
                                onOpenSettings()
                            },
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                    HorizontalDivider()
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("版本 ${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    closeDrawer()
                                    onCheckForUpdates()
                                },
                                enabled = !updateChecking,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            ) {
                                Text(if (updateChecking) "检查中…" else "检查更新", color = Purple, fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                closeDrawer()
                                onOpenGithub()
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("项目主页", color = Ink, fontSize = 14.sp)
                            Spacer(Modifier.weight(1f))
                            Icon(
                                painter = painterResource(R.drawable.ic_github),
                                contentDescription = "GitHub",
                                tint = Ink,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        },
    ) {
        if (settingsPageVisible) {
            UpdateSettingsScreen(
                enabled = autoUpdateEnabled,
                onEnabledChange = onAutoUpdateEnabledChange,
                onOpenMenu = { drawerScope.launch { drawerState.open() } },
                textageProgress = textageProgress,
                textageError = textageError,
                textageLastSyncAt = textageLastSyncAt,
                bjmMusicLastSyncAt = bjmMusicLastSyncAt,
                bjmScoresLastSyncAt = bjmScoresLastSyncAt,
                syncTarget = syncTarget,
                syncStage = syncStage,
                syncProgress = syncProgress,
                bjmLoggedIn = state.bjmUser != null,
                onFullDataSync = onFullDataSync,
                onSyncTextage = onSyncTextage,
                onSyncBjmMusic = onSyncBjmMusic,
                onSyncBjmScores = onSyncBjmScores,
                onClearChartCache = onClearChartCache,
            )
        } else if (bjmDataPageVisible) {
            BjmDataScreen(
                state = state,
                history = bjmHistory,
                bjmIndex = bjmIndex,
                onOpenMenu = { drawerScope.launch { drawerState.open() } },
                onLogin = onLogin,
                onLogout = onLogoutBjm,
                onOpenSong = { chart ->
                    onModeChange(chart.mode)
                    onOpenSongFromBjmHistory(chart)
                },
                query = bjmHistoryQuery,
                onQueryChange = { bjmHistoryQuery = it },
                selectedDate = bjmHistoryDate,
                onSelectedDateChange = { bjmHistoryDate = it },
                calendarExpanded = bjmHistoryCalendarExpanded,
                onCalendarExpandedChange = { bjmHistoryCalendarExpanded = it },
                calendarMonth = bjmHistoryCalendarMonth,
                onCalendarMonthChange = { bjmHistoryCalendarMonth = it },
                listState = bjmHistoryListState,
                modifier = modifier,
            )
        } else {
        val maxNumericVersionIndex = remember(state.charts, mode) {
            state.charts
                .asSequence()
                .filter { it.mode == mode }
                .mapNotNull { it.textageUrl?.let(::textageVersionIndex) }
                .maxOrNull()
                ?: -1
        }
        val substreamSortIndex = maxNumericVersionIndex + 1
        val versionOrder = remember(state.charts, mode) {
            state.charts
                .asSequence()
                .filter { it.mode == mode && it.version.isNotBlank() }
                .mapNotNull { chart ->
                    val index = chart.textageUrl?.let(::textageVersionIndex)
                        ?: chart.version.takeIf { it.equals("substream", ignoreCase = true) }
                            ?.let { substreamSortIndex }
                    index?.let { chart.version to it }
                }
                .toMap()
        }
        val versionOptions = remember(state.charts, mode, versionOrder) {
            state.charts
                .asSequence()
                .filter { it.mode == mode && it.version.isNotBlank() }
                .map { it.version }
                .distinct()
                .sortedWith(compareBy<String>({ versionOrder[it] ?: versionNumber(it) }, { it.lowercase(Locale.US) }))
                .toList()
        }
        val levelOptions = remember(state.charts, mode) {
            state.charts
                .asSequence()
                .filter { it.mode == mode && it.level > 0 }
                .map { it.level }
                .distinct()
                .sorted()
                .toList()
        }
        val chartsById = remember(state.charts) { state.charts.associateBy { it.id } }
        val allSongs = remember(state.songGroups, state.charts, mode) {
            state.songGroups.mapNotNull { group ->
                val charts = group.chartIds
                    .mapNotNull { chartId -> chartsById[chartId] }
                    .filter { it.mode == mode }
                if (charts.isEmpty()) {
                    null
                } else {
                    SongGroup(
                        key = group.key,
                        title = group.title,
                        subtitle = group.subtitle,
                        genre = group.genre,
                        composer = group.composer,
                        version = group.version,
                        sourceLabel = group.sourceLabel,
                        charts = charts,
                    )
                }
            }
        }
        val songs = remember(
            allSongs,
            query,
            selectedVersion,
            selectedLevel,
            searchGenreEnabled,
            searchTitleEnabled,
            searchComposerEnabled,
        ) {
            allSongs.mapNotNull { song ->
                val matchingCharts = song.charts.filter {
                    (selectedVersion == null || it.version == selectedVersion) &&
                        (selectedLevel == null || it.level == selectedLevel)
                }
                val searchText = buildList {
                    if (searchGenreEnabled) add(song.genre)
                    if (searchTitleEnabled) {
                        add(song.title)
                        add(song.subtitle)
                    }
                    if (searchComposerEnabled) add(song.composer)
                }.joinToString(" ")
                if (matchingCharts.isEmpty()) {
                    null
                } else if (
                    query.isNotBlank() &&
                    !searchText.contains(query, ignoreCase = true)
                ) {
                    null
                } else {
                    song.copy(
                        charts = matchingCharts
                            .groupBy { it.difficulty }
                            .values
                            .map { sameDifficulty ->
                                sameDifficulty.maxWithOrNull(
                                    compareBy<IidxChart>({ it.textageUrl != null }, { it.notes }, { it.bpm.isNotBlank() }),
                                ) ?: sameDifficulty.first()
                            }
                            .sortedWith(compareBy<IidxChart> { difficultyOrder(it.difficulty) }.thenBy { it.level }),
                    )
                }
            }
        }
        val selectedSearchDimensionCount = listOf(
            searchGenreEnabled,
            searchTitleEnabled,
            searchComposerEnabled,
        ).count { it }
        val searchDimensions = buildList {
            if (searchGenreEnabled) add("曲风")
            if (searchTitleEnabled) add("曲名")
            if (searchComposerEnabled) add("曲师")
        }
        val searchPlaceholder = when (searchDimensions.size) {
            1 -> "搜索${searchDimensions.first()}"
            2 -> "搜索${searchDimensions.joinToString("或")}"
            else -> "搜索${searchDimensions[0]}、${searchDimensions[1]}或${searchDimensions[2]}"
        }
        Column(modifier.fillMaxSize()) {
            AppTopBar(
                title = "曲库",
                onNavigate = { drawerScope.launch { drawerState.open() } },
                menu = true,
            ) {
                PlayStyleButton(mode) { onModeChange(if (mode == "SP") "DP" else "SP") }
                Spacer(Modifier.width(12.dp))
                val avatarText = state.bjmUser
                    ?.let { (it.name.ifBlank { it.id }).firstOrNull()?.toString()?.uppercase() }
                    ?: "○"
                Box(
                    Modifier.size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (state.bjmUser == null) Panel else Purple.copy(alpha = .18f))
                        .clickable(onClick = onOpenBjmData),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.bjmUser == null) {
                        Canvas(Modifier.size(22.dp)) {
                            drawCircle(Muted, radius = size.minDimension * .16f, center = Offset(size.width / 2f, size.height * .28f))
                            drawRoundRect(
                                color = Muted,
                                topLeft = Offset(size.width * .18f, size.height * .55f),
                                size = Size(size.width * .64f, size.height * .32f),
                                cornerRadius = CornerRadius(size.width * .16f),
                            )
                        }
                    } else {
                        Text(avatarText, color = Purple, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (textageProgress != null) TextageSyncBanner(textageProgress)

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(searchPlaceholder, color = Muted) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Text("×", color = Muted, fontSize = 20.sp)
                            }
                        }
                    },
                )
                IconButton(onClick = { filterExpanded = !filterExpanded }) {
                    FunnelIcon(
                        if (
                            filterExpanded ||
                            selectedVersion != null ||
                            selectedLevel != null ||
                            selectedSearchDimensionCount < 3
                        ) Purple else Muted,
                    )
                }
            }
            if (filterExpanded) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchDimensionCheckbox(
                        label = "曲风",
                        checked = searchGenreEnabled,
                        enabled = !searchGenreEnabled || selectedSearchDimensionCount > 1,
                        onCheckedChange = { searchGenreEnabled = it },
                        modifier = Modifier.weight(1f),
                    )
                    SearchDimensionCheckbox(
                        label = "曲名",
                        checked = searchTitleEnabled,
                        enabled = !searchTitleEnabled || selectedSearchDimensionCount > 1,
                        onCheckedChange = { searchTitleEnabled = it },
                        modifier = Modifier.weight(1f),
                    )
                    SearchDimensionCheckbox(
                        label = "曲师",
                        checked = searchComposerEnabled,
                        enabled = !searchComposerEnabled || selectedSearchDimensionCount > 1,
                        onCheckedChange = { searchComposerEnabled = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterDropdown(
                        value = selectedVersion ?: "全部版本",
                        options = listOf("全部版本") + versionOptions,
                        onSelect = { selectedVersion = it.takeUnless { option -> option == "全部版本" } },
                        modifier = Modifier.weight(1f),
                    )
                    FilterDropdown(
                        value = selectedLevel?.toString() ?: "全部等级",
                        options = listOf("全部等级") + levelOptions.map(Int::toString),
                        onSelect = { selectedLevel = it.toIntOrNull() },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            selectedVersion = null
                            selectedLevel = null
                            searchGenreEnabled = true
                            searchTitleEnabled = true
                            searchComposerEnabled = true
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) { Text("重置", color = Muted, fontSize = 11.sp) }
                }
            }
            val activeFilterSummary = buildString {
                selectedVersion?.let { append(it) }
                selectedLevel?.let {
                    if (isNotEmpty()) append("，")
                    append("LEVEL $it")
                }
            }
            val collapsedFilterSummary = buildString {
                if (selectedSearchDimensionCount < 3) {
                    append("仅筛选${searchDimensions.joinToString("/")}")
                }
                if (activeFilterSummary.isNotBlank()) {
                    if (isNotEmpty()) append("，")
                    append("已筛选：$activeFilterSummary")
                }
            }
            if (!filterExpanded && collapsedFilterSummary.isNotBlank()) {
                Text(
                    collapsedFilterSummary,
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            Text(
                "${songs.size} 首曲目 · $mode",
                color = Muted,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            if (songs.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("没有找到匹配曲目", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(8.dp))
                        Text("试试其他关键词，或重置版本和等级筛选", style = MaterialTheme.typography.bodySmall, color = Muted)
                        TextButton(onClick = { query = ""; selectedVersion = null; selectedLevel = null }) {
                            Text("清除筛选")
                        }
                    }
                }
            }
            if (songs.isNotEmpty()) LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(songs, key = { it.key }) { song ->
                    SongGroupRow(
                        song = song,
                        bjmIndex = bjmIndex,
                        onOpenSong = onOpenSong,
                        onOpenChart = onOpenChart,
                        onCopyText = onCopyText,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp),
                    )
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
        }
    }
}

@Composable
private fun FunnelIcon(color: ComposeColor) {
    Canvas(Modifier.size(22.dp)) {
        val path = Path().apply {
            moveTo(size.width * .12f, size.height * .18f)
            lineTo(size.width * .88f, size.height * .18f)
            lineTo(size.width * .60f, size.height * .52f)
            lineTo(size.width * .60f, size.height * .82f)
            lineTo(size.width * .40f, size.height * .70f)
            lineTo(size.width * .40f, size.height * .52f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 2f))
    }
}

@Composable
private fun FilterDropdown(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(value) { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(38.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text(value, color = Ink, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 12.sp) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

internal fun displayTitle(title: String, sourceLabel: String): String =
    listOf(title.trim(), sourceLabel.trim()).filter { it.isNotBlank() }.joinToString(" ")

internal data class SongGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val genre: String,
    val composer: String,
    val version: String,
    val sourceLabel: String,
    val charts: List<IidxChart>,
)

@Composable
private fun SearchDimensionCheckbox(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.size(28.dp),
        )
        Text(
            label,
            color = if (enabled || checked) Ink else Muted,
            fontSize = 11.sp,
        )
    }
}
