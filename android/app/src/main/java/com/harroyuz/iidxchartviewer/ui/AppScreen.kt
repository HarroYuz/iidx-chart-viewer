package com.harroyuz.iidxchartviewer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import com.harroyuz.iidxchartviewer.ui.components.RetainedPage
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.harroyuz.iidxchartviewer.ui.motion.BrowseMotion
import com.harroyuz.iidxchartviewer.ui.motion.LocalBrowseMotion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.harroyuz.iidxchartviewer.data.remote.update.GithubReleaseInfo
import com.harroyuz.iidxchartviewer.domain.catalog.difficultyOrder
import com.harroyuz.iidxchartviewer.domain.catalog.songGroupKey
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxAppState
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.model.TextageChartData
import com.harroyuz.iidxchartviewer.domain.model.TextageSyncProgress
import com.harroyuz.iidxchartviewer.domain.player.PlayerSettings
import com.harroyuz.iidxchartviewer.domain.sync.DataSyncTarget
import com.harroyuz.iidxchartviewer.ui.catalog.ChartBrowserScreen
import com.harroyuz.iidxchartviewer.ui.catalog.SongDetailScreen
import com.harroyuz.iidxchartviewer.ui.components.LocalDataLoadingScreen
import com.harroyuz.iidxchartviewer.ui.components.TextageBootstrapScreen
import com.harroyuz.iidxchartviewer.ui.components.ToastCard
import com.harroyuz.iidxchartviewer.ui.player.ChartDetailScreen
import com.harroyuz.iidxchartviewer.ui.settings.UpdateDialog
import com.harroyuz.iidxchartviewer.ui.theme.Background

private data class BrowsePage(
    val song: IidxChart?,
    val chart: IidxChart?,
    val chartData: TextageChartData?,
    val chartLoading: Boolean,
) {
    val key: String get() = when {
        chart != null -> "chart"
        song != null -> "song"
        else -> "browser"
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun IidxApp(
    state: IidxAppState,
    bjmHistory: List<BjmScore>,
    bjmIndex: BjmIndex,
    localCatalogPresent: Boolean,
    textageSyncing: Boolean,
    textageProgress: TextageSyncProgress?,
    textageError: String?,
    textageLastSyncAt: Long,
    bjmMusicLastSyncAt: Long,
    bjmScoresLastSyncAt: Long,
    selectedSong: IidxChart?,
    selectedChart: IidxChart?,
    chartData: TextageChartData?,
    chartLoading: Boolean,
    spPlayerSettings: PlayerSettings,
    dpPlayerSettings: PlayerSettings,
    message: String?,
    onDismissMessage: () -> Unit,
    visualEffectsDisabled: Boolean,
    onVisualEffectsDisabledChange: (Boolean) -> Unit,
    autoUpdateEnabled: Boolean,
    localDataLoading: Boolean,
    localDataProgress: Float,
    localDataStage: String,
    updateChecking: Boolean,
    updateInfo: GithubReleaseInfo?,
    updateDownloadProgress: Float?,
    updateInstalling: Boolean,
    syncTarget: DataSyncTarget?,
    syncStage: String?,
    syncProgress: Float?,
    settingsPageVisible: Boolean,
    onLogin: () -> Unit,
    onLogoutBjm: () -> Unit,
    onOpenBjmData: () -> Unit,
    onRefreshTextage: () -> Unit,
    onFullDataSync: () -> Unit,
    onSyncTextage: () -> Unit,
    onSyncBjmMusic: () -> Unit,
    onSyncBjmScores: () -> Unit,
    onOpenGithub: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    bjmDataPageVisible: Boolean,
    onDismissBjmData: () -> Unit,
    onAutoUpdateEnabledChange: (Boolean) -> Unit,
    onClearChartCache: () -> Unit,
    onDismissUpdate: () -> Unit,
    onDownloadUpdate: (GithubReleaseInfo) -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    onOpenSong: (IidxChart) -> Unit,
    onOpenSongFromBjmHistory: (IidxChart) -> Unit,
    onCopyText: (String) -> Unit,
    onBack: () -> Unit,
    onRequestExit: () -> Unit,
    onRetryChart: () -> Unit,
    onPlayerSettingsChange: (String, PlayerSettings) -> Unit,
) {
    Scaffold(containerColor = Background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            var browserMode by rememberSaveable { mutableStateOf("SP") }
            val chartsById = remember(state.charts) { state.charts.associateBy { it.id } }
            val chartsBySongKey = remember(state.songGroups, state.charts) {
                state.songGroups.associate { group ->
                    group.key to group.chartIds.mapNotNull { chartId -> chartsById[chartId] }
                }
            }
            val showingBootstrap = !localCatalogPresent && (state.charts.isEmpty() || textageProgress?.initial == true)
            if (localDataLoading) {
                LocalDataLoadingScreen(
                    progress = localDataProgress,
                    stage = localDataStage,
                )
            } else if (showingBootstrap) {
                TextageBootstrapScreen(
                    progress = textageProgress,
                    error = textageError,
                    retryEnabled = !textageSyncing,
                    onRetry = onRefreshTextage,
                )
            } else {
                val pageState = rememberSaveableStateHolder()
                val showingDetail = selectedSong != null || selectedChart != null
                val currentPage = BrowsePage(selectedSong, selectedChart, chartData, chartLoading)
                BackHandler(showingDetail, onBack = onBack)
                SharedTransitionLayout(Modifier.fillMaxSize().clipToBounds()) {
                    val pageTransition = updateTransition(currentPage, label = "browse")
                    val betweenDetails = pageTransition.currentState.key != "browser" &&
                        pageTransition.targetState.key != "browser"
                    val browserParticipating = pageTransition.currentState.key == "browser" ||
                        pageTransition.targetState.key == "browser"
                    val movingSongKey = (pageTransition.targetState.chart ?: pageTransition.targetState.song
                        ?: pageTransition.currentState.chart ?: pageTransition.currentState.song)?.let(::songGroupKey)
                    CompositionLocalProvider(
                        LocalBrowseMotion provides if (visualEffectsDisabled || !browserParticipating) null
                        else BrowseMotion(this, null, false, callerVisible = !showingDetail, movingSongKey = movingSongKey),
                    ) {
                        RetainedPage(
                            visible = browserParticipating,
                            interactive = !showingDetail,
                        ) {
                            ChartBrowserScreen(
                                state = state,
                                bjmHistory = bjmHistory,
                                bjmIndex = bjmIndex,
                                mode = browserMode,
                                onModeChange = { browserMode = it },
                                textageSyncing = textageSyncing,
                                textageProgress = textageProgress,
                                textageError = textageError,
                                textageLastSyncAt = textageLastSyncAt,
                                bjmMusicLastSyncAt = bjmMusicLastSyncAt,
                                bjmScoresLastSyncAt = bjmScoresLastSyncAt,
                                onLogin = onLogin,
                                onLogoutBjm = onLogoutBjm,
                                onOpenBjmData = onOpenBjmData,
                                onRefreshTextage = onRefreshTextage,
                                onFullDataSync = onFullDataSync,
                                onSyncTextage = onSyncTextage,
                                onSyncBjmMusic = onSyncBjmMusic,
                                onSyncBjmScores = onSyncBjmScores,
                                onOpenGithub = onOpenGithub,
                                onCheckForUpdates = onCheckForUpdates,
                                updateChecking = updateChecking,
                                syncTarget = syncTarget,
                                syncStage = syncStage,
                                syncProgress = syncProgress,
                                settingsPageVisible = settingsPageVisible,
                                onOpenSettings = onOpenSettings,
                                onDismissSettings = onDismissSettings,
                                bjmDataPageVisible = bjmDataPageVisible,
                                onDismissBjmData = onDismissBjmData,
                                onOpenSongFromBjmHistory = onOpenSongFromBjmHistory,
                                visualEffectsDisabled = visualEffectsDisabled,
                                onVisualEffectsDisabledChange = onVisualEffectsDisabledChange,
                                autoUpdateEnabled = autoUpdateEnabled,
                                onAutoUpdateEnabledChange = onAutoUpdateEnabledChange,
                                onClearChartCache = onClearChartCache,
                                onOpenChart = onOpenChart,
                                onOpenSong = onOpenSong,
                                onCopyText = onCopyText,
                                showingDetail = showingDetail,
                                onBack = onBack,
                                onRequestExit = onRequestExit,
                            )
                        }
                    }
                    pageTransition.AnimatedContent(
                        contentKey = { it.key },
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            if (visualEffectsDisabled || (initialState.key != "browser" && targetState.key != "browser")) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                fadeIn(tween(147, delayMillis = 60)) togetherWith fadeOut(tween(120))
                            }.using(null)
                        },
                    ) { page ->
                        CompositionLocalProvider(
                            LocalBrowseMotion provides if (visualEffectsDisabled) null else BrowseMotion(this@SharedTransitionLayout, this, betweenDetails),
                        ) {
                            pageState.SaveableStateProvider(page.key) {
                                Box(Modifier.fillMaxSize().then(
                                    if (page.key != currentPage.key) Modifier.pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                            }
                                        }
                                    } else Modifier,
                                )) {
                                    val selectedSong = page.song
                                    val selectedChart = page.chart
                                    if (selectedChart != null) {
                                        val selectedSongKey = songGroupKey(selectedChart)
                                        val songCharts = chartsBySongKey[selectedSongKey].orEmpty()
                                        val family = songCharts
                                            .filter {
                                                it.mode == selectedChart.mode
                                            }
                                            .groupBy { it.difficulty }
                                            .values
                                            .mapNotNull { sameDifficulty ->
                                                sameDifficulty.maxWithOrNull(
                                                    compareBy<IidxChart>({ it.textageUrl != null }, { it.notes }, { it.bpm.isNotBlank() }),
                                                )
                                            }
                                            .sortedWith(compareBy<IidxChart> { difficultyOrder(it.difficulty) }.thenBy { it.level })
                                        ChartDetailScreen(
                                            chart = selectedChart,
                                            siblingCharts = family,
                                            bjmIndex = bjmIndex,
                                            chartData = page.chartData,
                                            loading = page.chartLoading,
                                            playerSettings = if (selectedChart.mode == "DP") dpPlayerSettings else spPlayerSettings,
                                            onBack = onBack,
                                            onRetry = onRetryChart,
                                            mode = browserMode,
                                            onStyleToggle = {
                                                val targetMode = if (browserMode == "SP") "DP" else "SP"
                                                val alternate = songCharts
                                                    .filter {
                                                        it.mode == targetMode &&
                                                            it.textageUrl != null
                                                    }
                                                    .maxWithOrNull(
                                                        compareBy<IidxChart>({ difficultyOrder(it.difficulty) }, { it.level }, { it.notes }),
                                                    )
                                                browserMode = targetMode
                                                if (alternate != null) onOpenChart(alternate) else onBack()
                                            },
                                            onOpenChart = onOpenChart,
                                            onCopyText = onCopyText,
                                            onPlayerSettingsChange = { settings ->
                                                onPlayerSettingsChange(selectedChart.mode, settings)
                                            },
                                        )
                                    } else if (selectedSong != null) {
                                        val selectedSongKey = songGroupKey(selectedSong)
                                        val songCharts = chartsBySongKey[selectedSongKey].orEmpty()
                                        val family = songCharts
                                            .filter {
                                                it.mode == selectedSong.mode
                                            }
                                            .groupBy { it.difficulty }
                                            .values
                                            .mapNotNull { sameDifficulty ->
                                                sameDifficulty.maxWithOrNull(
                                                    compareBy<IidxChart>({ it.textageUrl != null }, { it.notes }, { it.bpm.isNotBlank() }),
                                                )
                                            }
                                            .sortedWith(compareBy<IidxChart> { difficultyOrder(it.difficulty) }.thenBy { it.level })
                                        SongDetailScreen(
                                            song = selectedSong,
                                            charts = family,
                                            bjmIndex = bjmIndex,
                                            mode = browserMode,
                                            onBack = onBack,
                                            onStyleToggle = {
                                                val targetMode = if (browserMode == "SP") "DP" else "SP"
                                                val alternate = songCharts
                                                    .filter {
                                                        it.mode == targetMode
                                                    }
                                                    .maxWithOrNull(
                                                        compareBy<IidxChart>({ it.textageUrl != null }, { difficultyOrder(it.difficulty) }, { it.level }, { it.notes }),
                                                    )
                                                if (alternate != null) {
                                                    browserMode = targetMode
                                                    onOpenSong(alternate)
                                                }
                                            },
                                            onOpenChart = onOpenChart,
                                            onCopyText = onCopyText,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (message != null) ToastCard(message, onDismissMessage, Modifier.align(Alignment.BottomCenter))
            updateInfo?.let { release ->
                UpdateDialog(
                    release = release,
                    downloadProgress = updateDownloadProgress,
                    installing = updateInstalling,
                    onDismiss = onDismissUpdate,
                    onDownload = { onDownloadUpdate(release) },
                )
            }
        }
    }
}
