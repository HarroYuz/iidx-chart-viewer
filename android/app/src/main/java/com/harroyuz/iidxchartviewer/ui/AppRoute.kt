package com.harroyuz.iidxchartviewer.ui

import androidx.compose.runtime.Composable
import com.harroyuz.iidxchartviewer.app.AppViewModel

@Composable
internal fun AppRoute(viewModel: AppViewModel) {
    with(viewModel) {
        IidxApp(
            state = appState,
            bjmHistory = bjmHistory,
            bjmIndex = bjmIndex,
            textageSyncing = textageSyncing,
            textageProgress = textageProgress,
            textageError = textageError,
            textageLastSyncAt = textageLastSyncAt,
            bjmMusicLastSyncAt = bjmMusicLastSyncAt,
            bjmScoresLastSyncAt = bjmScoresLastSyncAt,
            selectedSong = selectedSong,
            selectedChart = selectedChart,
            chartData = selectedChartData,
            chartLoading = chartLoading,
            spPlayerSettings = spPlayerSettings,
            dpPlayerSettings = dpPlayerSettings,
            message = message,
            localCatalogPresent = localCatalogPresent,
            localDataLoading = localDataLoading,
            localDataProgress = localDataProgress,
            localDataStage = localDataStage,
            visualEffectsDisabled = visualEffectsDisabled,
            onVisualEffectsDisabledChange = ::changeVisualEffectsDisabled,
            autoUpdateEnabled = autoUpdateEnabled,
            updateChecking = updateChecking,
            updateInfo = updateInfo,
            updateDownloadProgress = updateDownloadProgress,
            updateInstalling = updateInstalling,
            syncTarget = syncTarget,
            syncStage = syncStage,
            syncProgress = syncProgress,
            onDismissMessage = ::dismissMessage,
            onLogin = ::openBjmLogin,
            onLogoutBjm = ::logoutBjm,
            onOpenBjmData = { showBjmData(true) },
            onRefreshTextage = ::syncTextageOnly,
            onFullDataSync = { syncAllData() },
            onSyncTextage = ::syncTextageOnly,
            onSyncBjmMusic = ::syncBjmMusicOnly,
            onSyncBjmScores = ::syncBjmScoresOnly,
            onOpenGithub = ::openGithub,
            onCheckForUpdates = { checkForUpdates(manual = true) },
            settingsPageVisible = settingsPageVisible,
            onOpenSettings = { showSettings(true) },
            onDismissSettings = { showSettings(false) },
            bjmDataPageVisible = bjmDataPageVisible,
            onDismissBjmData = { showBjmData(false) },
            onAutoUpdateEnabledChange = ::changeAutoUpdate,
            onClearChartCache = ::clearChartCache,
            onDismissUpdate = ::dismissUpdate,
            onDownloadUpdate = ::downloadUpdate,
            onOpenChart = ::openChart,
            onOpenSong = ::openSong,
            onOpenSongFromBjmHistory = { chart ->
                openSong(chart, fromBjmHistory = true)
            },
            onCopyText = ::copyText,
            onBack = ::handleBack,
            onRequestExit = ::requestExit,
            onRetryChart = { selectedChart?.let(::openChart) },
            onPlayerSettingsChange = ::savePlayerSettings,
        )
    }
}
