package com.harroyuz.iidxchartviewer.ui.catalog

import com.harroyuz.iidxchartviewer.domain.catalog.preferredChartOrder
import com.harroyuz.iidxchartviewer.ui.components.RetainedPage
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.harroyuz.iidxchartviewer.domain.catalog.buildCatalogVersionOptions
import com.harroyuz.iidxchartviewer.domain.catalog.catalogSongTypes
import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.domain.catalog.matchesCatalogFilters
import com.harroyuz.iidxchartviewer.ui.motion.LocalBrowseMotion
import com.harroyuz.iidxchartviewer.ui.motion.browseCatalogItem
import com.harroyuz.iidxchartviewer.ui.motion.browseCatalogChrome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.BuildConfig
import com.harroyuz.iidxchartviewer.R
import com.harroyuz.iidxchartviewer.domain.catalog.difficultyOrder
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
    visualEffectsDisabled: Boolean,
    onVisualEffectsDisabledChange: (Boolean) -> Unit,
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
    var selectedVersions by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedLevels by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var includeDeleted by rememberSaveable { mutableStateOf(true) }
    var includeConsumer by rememberSaveable { mutableStateOf(true) }
    var includeCurrent by rememberSaveable { mutableStateOf(true) }
    val selectedTypes = buildSet {
        if (includeDeleted) add(ArcadeStatus.DELETED)
        if (includeConsumer) add(ArcadeStatus.CONSUMER_ONLY)
        if (includeCurrent) add(ArcadeStatus.CURRENT)
    }
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
        Box(Modifier.fillMaxSize()) {
            RetainedPage(visible = !settingsPageVisible && !bjmDataPageVisible) {
                val versionOptions = remember(state.charts, mode) {
                    buildCatalogVersionOptions(state.charts.filter { it.mode == mode })
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
                    selectedVersions,
                    selectedLevels,
                    selectedTypes,
                    searchGenreEnabled,
                    searchTitleEnabled,
                    searchComposerEnabled,
                ) {
                    val versions = selectedVersions.toSet()
                    val levels = selectedLevels.toSet()
                    allSongs.mapNotNull { song ->
                        val matchingCharts = song.charts.filter {
                            it.matchesCatalogFilters(versions, levels, selectedTypes)
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
                                            preferredChartOrder,
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
                val movingSongKey = LocalBrowseMotion.current?.movingSongKey
                val movingIndex = remember(songs, movingSongKey) { songs.indexOfFirst { it.key == movingSongKey } }
                Column(modifier.fillMaxSize()) {
                    Column(Modifier.browseCatalogChrome()) {
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
                                        selectedVersions.isNotEmpty() ||
                                        selectedLevels.isNotEmpty() ||
                                        selectedTypes.size < catalogSongTypes.size ||
                                        selectedSearchDimensionCount < 3
                                    ) Purple else Muted,
                                )
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
                                searchFields = searchDimensions,
                                onSearchFieldToggle = { field ->
                                    when (field) {
                                        "曲风" -> if (!searchGenreEnabled || selectedSearchDimensionCount > 1) searchGenreEnabled = !searchGenreEnabled
                                        "曲名" -> if (!searchTitleEnabled || selectedSearchDimensionCount > 1) searchTitleEnabled = !searchTitleEnabled
                                        "曲师" -> if (!searchComposerEnabled || selectedSearchDimensionCount > 1) searchComposerEnabled = !searchComposerEnabled
                                    }
                                },
                                types = selectedTypes,
                                onTypeToggle = { type ->
                                    when (type) {
                                        ArcadeStatus.DELETED -> includeDeleted = !includeDeleted
                                        ArcadeStatus.CONSUMER_ONLY -> includeConsumer = !includeConsumer
                                        ArcadeStatus.CURRENT -> includeCurrent = !includeCurrent
                                        ArcadeStatus.UNKNOWN -> Unit
                                    }
                                },
                                levels = selectedLevels.toSet(),
                                onLevelToggle = { selectedLevels = if (it in selectedLevels) selectedLevels - it else selectedLevels + it },
                                versions = selectedVersions.toSet(),
                                versionOptions = versionOptions,
                                onVersionToggle = { selectedVersions = if (it in selectedVersions) selectedVersions - it else selectedVersions + it },
                                onClearLevels = { selectedLevels = emptyList() },
                                onClearVersions = { selectedVersions = emptyList() },
                                onReset = {
                                    selectedVersions = emptyList()
                                    selectedLevels = emptyList()
                                    includeDeleted = true
                                    includeConsumer = true
                                    includeCurrent = true
                                    searchGenreEnabled = true
                                    searchTitleEnabled = true
                                    searchComposerEnabled = true
                                },
                                visualEffectsDisabled = visualEffectsDisabled,
                            )
                        }
                        val activeFilterSummary = buildList {
                            if (selectedVersions.isNotEmpty()) {
                                add(versionOptions.filter { it.value in selectedVersions }.joinToString(" / ") { it.label })
                            }
                            if (selectedLevels.isNotEmpty()) add("LEVEL ${selectedLevels.sorted().joinToString("/")}")
                            if (selectedTypes.size < catalogSongTypes.size) {
                                add(if (selectedTypes.isEmpty()) "未选择曲目类型" else
                                    "曲目类型：${catalogSongTypes.filter { it in selectedTypes }.joinToString("/") { it.filterLabel() }}")
                            }
                        }.joinToString("，")
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
                    }
                    if (songs.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("没有找到匹配曲目", style = MaterialTheme.typography.titleMedium, color = Ink)
                                Spacer(Modifier.height(8.dp))
                                Text("试试其他关键词，或重置筛选", style = MaterialTheme.typography.bodySmall, color = Muted)
                                TextButton(onClick = {
                                    query = ""
                                    selectedVersions = emptyList()
                                    selectedLevels = emptyList()
                                    includeDeleted = true
                                    includeConsumer = true
                                    includeCurrent = true
                                }) {
                                    Text("清除筛选")
                                }
                            }
                        }
                    }
                    if (songs.isNotEmpty()) LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        itemsIndexed(songs, key = { _, song -> song.key }) { index, song ->
                            SongGroupRow(
                                song = song,
                                bjmIndex = bjmIndex,
                                onOpenSong = onOpenSong,
                                onOpenChart = onOpenChart,
                                onCopyText = onCopyText,
                                modifier = Modifier.browseCatalogItem(song.key, aboveSelected = index < movingIndex)
                                    .padding(horizontal = 18.dp, vertical = 5.dp),
                            )
                        }
                        item { Spacer(Modifier.height(18.dp)) }
                    }
                }
            }
            RetainedPage(visible = bjmDataPageVisible) {
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
            }
            if (settingsPageVisible) {
                UpdateSettingsScreen(
                    visualEffectsDisabled = visualEffectsDisabled,
                    onVisualEffectsDisabledChange = onVisualEffectsDisabledChange,
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
