package com.harroyuz.iidxchartviewer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
                // Keep the browser composed behind the detail screen so its
                // query, style and LazyColumn position survive a round trip.
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
                    autoUpdateEnabled = autoUpdateEnabled,
                    onAutoUpdateEnabledChange = onAutoUpdateEnabledChange,
                    onClearChartCache = onClearChartCache,
                    onOpenChart = onOpenChart,
                    onOpenSong = onOpenSong,
                    onCopyText = onCopyText,
                    showingDetail = selectedSong != null || selectedChart != null,
                    onBack = onBack,
                    onRequestExit = onRequestExit,
                    modifier = Modifier
                        .alpha(if (selectedSong == null && selectedChart == null) 1f else 0f)
                        .then(
                            if (selectedSong != null || selectedChart != null) {
                                Modifier.pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { change ->
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            },
                        ),
                )
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
                        chartData = chartData,
                        loading = chartLoading,
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
