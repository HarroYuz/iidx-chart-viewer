package com.harroyuz.iidxchartviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import java.util.Locale

private val Ink = ComposeColor(0xFF171722)
private val Muted = ComposeColor(0xFF6E6C7A)
private val Background = ComposeColor(0xFFFFFFFF)
private val Panel = ComposeColor(0xFFF1F0F7)
private val Purple = ComposeColor(0xFF7353D6)
private val Cyan = ComposeColor(0xFF198D9B)
private val Orange = ComposeColor(0xFFE16035)
private val Green = ComposeColor(0xFF1B9B62)
private val NormalBlue = ComposeColor(0xFF3179D6)
private val PlayerBackground = ComposeColor(0xFF08090F)
private val PlayerLane = ComposeColor(0xFF252735)
private val PlayerCenterGap = ComposeColor(0xFF3A3D49)
private val PlayerEdge = ComposeColor(0xFF55596A)
private val PlayerRed = ComposeColor(0xFFFF1D2E)
private val PlayerSkyBlue = ComposeColor(0xFF28A9E0)
private val PlayerBpmGreen = ComposeColor(0xFF63D38A)
private val PlayerMeasureText = ComposeColor(0xFFB7BAC6)
private const val TEXTAGE_SYNC_INTERVAL_MS = 24L * 60L * 60L * 1000L
private const val UPDATE_CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L

private enum class DataSyncTarget {
    FULL,
    TEXTAGE,
    BJM_ALL,
    BJM_MUSIC,
    BJM_SCORES,
}

class MainActivity : ComponentActivity() {
    private lateinit var store: IidxLocalStore
    private lateinit var bjmClient: BjmClient
    private lateinit var textageClient: TextageClient
    private lateinit var githubUpdateClient: GithubUpdateClient

    private var appState by mutableStateOf(IidxAppState())
    private var bjmIndex by mutableStateOf(BjmIndex())
    private var localCatalogPresent by mutableStateOf(false)
    private var syncTarget by mutableStateOf<DataSyncTarget?>(null)
    private var syncStage by mutableStateOf<String?>(null)
    private var textageSyncing by mutableStateOf(false)
    private var textageProgress by mutableStateOf<TextageSyncProgress?>(null)
    private var textageError by mutableStateOf<String?>(null)
    private var chartLoading by mutableStateOf(false)
    private var selectedSong by mutableStateOf<IidxChart?>(null)
    private var selectedChart by mutableStateOf<IidxChart?>(null)
    private var selectedChartData by mutableStateOf<TextageChartData?>(null)
    private var playerSettings by mutableStateOf(PlayerSettings())
    private var message by mutableStateOf<String?>(null)
    private var autoUpdateEnabled by mutableStateOf(true)
    private var updateChecking by mutableStateOf(false)
    private var updateInfo by mutableStateOf<GithubReleaseInfo?>(null)
    private var updateDownloadProgress by mutableStateOf<Float?>(null)
    private var updateInstalling by mutableStateOf(false)
    private var settingsPageVisible by mutableStateOf(false)
    private var loginPending = false
    private var exitToastShown = false

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loginPending = false
            message = "BJM 登录完成，正在同步成绩…"
            syncBjm()
        } else {
            loginPending = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        store = IidxLocalStore(this)
        localCatalogPresent = store.hasTextageCatalogMarker()
        bjmClient = BjmClient()
        textageClient = TextageClient()
        githubUpdateClient = GithubUpdateClient()
        autoUpdateEnabled = store.autoUpdateEnabled()
        appState = IidxAppState()
        playerSettings = store.loadPlayerSettings()

        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                background = Background,
                surface = Panel,
                primary = Purple,
                onPrimary = ComposeColor.White,
                onBackground = Ink,
                onSurface = Ink,
            )) {
                IidxApp(
                    state = appState,
                    bjmIndex = bjmIndex,
                    textageSyncing = textageSyncing,
                    textageProgress = textageProgress,
                    textageError = textageError,
                    selectedSong = selectedSong,
                    selectedChart = selectedChart,
                    chartData = selectedChartData,
                    chartLoading = chartLoading,
                    playerSettings = playerSettings,
                    message = message,
                    localCatalogPresent = localCatalogPresent,
                    autoUpdateEnabled = autoUpdateEnabled,
                    updateChecking = updateChecking,
                    updateInfo = updateInfo,
                    updateDownloadProgress = updateDownloadProgress,
                    updateInstalling = updateInstalling,
                    syncTarget = syncTarget,
                    syncStage = syncStage,
                    onDismissMessage = { message = null },
                    onLogin = ::openBjmLogin,
                    onOpenBjmProfile = {},
                    onRefreshTextage = ::refreshTextage,
                    onFullDataSync = ::syncAllData,
                    onSyncTextage = ::syncTextageOnly,
                    onSyncBjmMusic = ::syncBjmMusicOnly,
                    onSyncBjmScores = ::syncBjmScoresOnly,
                    onOpenGithub = ::openGithub,
                    onCheckForUpdates = { checkForUpdates(manual = true) },
                    settingsPageVisible = settingsPageVisible,
                    onOpenSettings = { settingsPageVisible = true },
                    onDismissSettings = { settingsPageVisible = false },
                    onAutoUpdateEnabledChange = {
                        autoUpdateEnabled = it
                        store.setAutoUpdateEnabled(it)
                    },
                    onClearChartCache = ::clearChartCache,
                    onDismissUpdate = { if (updateDownloadProgress == null && !updateInstalling) updateInfo = null },
                    onDownloadUpdate = ::downloadUpdate,
                    onOpenChart = ::openChart,
                    onOpenSong = ::openSong,
                    onCopyText = ::copyText,
                    onBack = ::handleBack,
                    onRequestExit = ::requestExit,
                    onRetryChart = { selectedChart?.let(::openChart) },
                    onPlayerSettingsChange = ::savePlayerSettings,
                )
            }
        }
        // Loading a large local catalog is also kept off the main thread.
        lifecycleScope.launch(Dispatchers.IO) {
            val loaded = store.load()
            val loadedBjmIndex = store.loadBjmIndex() ?: BjmIndex()
            withContext(Dispatchers.Main) {
                appState = loaded
                bjmIndex = loadedBjmIndex
                lifecycleScope.launch(Dispatchers.Default) {
                    val refreshedIndex = refreshCachedBjmIndex(loaded, loadedBjmIndex, store)
                    withContext(Dispatchers.Main) {
                        if (
                            appState.charts === loaded.charts &&
                            appState.bjmMusic === loaded.bjmMusic &&
                            appState.bjmScores === loaded.bjmScores
                        ) {
                            bjmIndex = refreshedIndex
                            withContext(Dispatchers.IO) { store.saveBjmIndex(refreshedIndex) }
                        }
                    }
                }
                // Bootstrap only when there is no usable local catalog. A
                // completed catalog should open immediately; the update
                // button performs the next metadata refresh on demand.
                val needsBootstrap = loaded.charts.isEmpty() || !store.isTextageSyncComplete()
                val dailySyncDue = System.currentTimeMillis() - store.fullSyncLastAt() >= TEXTAGE_SYNC_INTERVAL_MS
                if (needsBootstrap || dailySyncDue) {
                    syncAllData()
                }
                checkForUpdatesIfDue()
            }
        }
    }

    private fun openBjmLogin() {
        loginPending = true
        loginLauncher.launch(Intent(this, BjmLoginActivity::class.java))
    }

    private fun openGithub() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HarroYuz/iidx-chart-viewer")))
    }

    private fun copyText(value: String) {
        val text = value.trim()
        if (text.isBlank()) return
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("IIDX Data", text))
        message = "已复制：$text"
    }

    private fun checkForUpdatesIfDue() {
        if (autoUpdateEnabled && System.currentTimeMillis() - githubUpdateLastCheckAt() >= UPDATE_CHECK_INTERVAL_MS) {
            checkForUpdates(manual = false)
        }
    }

    private fun githubUpdateLastCheckAt(): Long = store.updateLastCheckAt()

    private fun checkForUpdates(manual: Boolean) {
        if (updateChecking) return
        updateChecking = true
        lifecycleScope.launch {
            try {
                val release = withContext(Dispatchers.IO) { githubUpdateClient.fetchLatestRelease() }
                store.setUpdateLastCheckAt()
                if (GithubUpdateClient.isNewer(BuildConfig.VERSION_NAME, release.tagName)) {
                    updateInfo = release
                } else if (manual) {
                    message = "当前已是最新版本 ${BuildConfig.VERSION_NAME}"
                }
            } catch (error: Exception) {
                if (manual) message = error.message ?: "更新检查失败"
            } finally {
                updateChecking = false
            }
        }
    }

    private fun downloadUpdate(release: GithubReleaseInfo) {
        if (updateDownloadProgress != null || updateInstalling) return
        updateDownloadProgress = 0f
        lifecycleScope.launch {
            try {
                val apk = githubUpdateClient.downloadApk(this@MainActivity, release) { downloaded, total ->
                    withContext(Dispatchers.Main) {
                        updateDownloadProgress = if (total > 0L) {
                            (downloaded.toFloat() / total).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                    }
                }
                updateDownloadProgress = 1f
                updateInstalling = true
                installApk(apk)
                updateInfo = null
            } catch (error: Exception) {
                message = error.message ?: "APK 下载失败"
            } finally {
                updateDownloadProgress = null
                updateInstalling = false
            }
        }
    }

    private fun installApk(apk: File) {
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", apk)
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }

    private fun startDataSync(target: DataSyncTarget, action: suspend () -> Unit) {
        if (syncTarget != null) return
        syncTarget = target
        syncStage = null
        lifecycleScope.launch {
            try {
                action()
            } catch (error: Exception) {
                message = error.message ?: "数据同步失败"
            } finally {
                syncStage = null
                syncTarget = null
            }
        }
    }

    private fun syncBjm() {
        startDataSync(DataSyncTarget.BJM_ALL) {
            syncStage = "正在同步 BJM 曲目库"
            syncBjmMusicData()
            syncStage = "正在同步用户成绩"
            val count = syncBjmScoresData()
            syncStage = "正在构建索引"
            rebuildFullBjmIndex()
            message = "已同步 $count 条 BJM 成绩"
        }
    }

    private fun syncAllData() {
        startDataSync(DataSyncTarget.FULL) {
            syncStage = "正在同步 Textage 歌曲库"
            syncTextageData()
            if (appState.bjmUser != null) {
                syncStage = "正在同步 BJM 曲目库"
                syncBjmMusicData()
                syncStage = "正在同步用户成绩"
                syncBjmScoresData()
            }
            syncStage = "正在构建索引"
            rebuildFullBjmIndex()
            store.setFullSyncLastAt()
            message = "全量数据同步完成"
        }
    }

    private fun refreshTextage() = syncTextageOnly()

    private fun syncTextageOnly() {
        startDataSync(DataSyncTarget.TEXTAGE) {
            syncStage = "正在同步 Textage 歌曲库"
            syncTextageData()
            syncStage = "正在构建索引"
            rebuildTextageBjmIndex()
            message = "Textage 歌曲库同步完成"
        }
    }

    private fun syncBjmMusicOnly() {
        if (appState.bjmUser == null) {
            message = "请先登录 BJM"
            return
        }
        startDataSync(DataSyncTarget.BJM_MUSIC) {
            syncStage = "正在同步 BJM 曲目库"
            syncBjmMusicData()
            syncStage = "正在构建索引"
            rebuildBjmMusicIndex()
            message = "BJM 曲目库同步完成"
        }
    }

    private fun syncBjmScoresOnly() {
        if (appState.bjmUser == null) {
            message = "请先登录 BJM"
            return
        }
        startDataSync(DataSyncTarget.BJM_SCORES) {
            syncStage = "正在同步用户成绩"
            val count = syncBjmScoresData()
            syncStage = "正在构建索引"
            rebuildBjmScoresIndex()
            message = "已同步 $count 条 BJM 成绩"
        }
    }

    private suspend fun syncTextageData() {
        val initial = appState.charts.isEmpty() || !store.isTextageSyncComplete()
        textageSyncing = true
        textageError = null
        textageProgress = TextageSyncProgress(initial, 0, 0, "正在获取全部歌曲元数据…")
        try {
            var lastProgressPublishedAt = 0L
            var lastProgressCompleted = 0
            val imported = textageClient.fetchCatalog { completed, total, title ->
                val now = SystemClock.uptimeMillis()
                val publish = completed == total ||
                    now - lastProgressPublishedAt >= 120L ||
                    completed - lastProgressCompleted >= 32
                if (publish) {
                    lastProgressPublishedAt = now
                    lastProgressCompleted = completed
                    withContext(Dispatchers.Main.immediate) {
                        textageProgress = TextageSyncProgress(initial, completed, total, title)
                    }
                }
            }
            if (imported.isEmpty()) throw TextageException("Textage 没有返回可识别的谱面目录")
            val old = appState.charts.associateBy { it.id }
            val merged = imported.map { incoming ->
                old[incoming.id]?.let { previous ->
                    incoming.copy(
                        notes = if (incoming.notes > 0) incoming.notes else previous.notes,
                        score = previous.score,
                        confirmed = previous.confirmed,
                    )
                } ?: incoming
            }
            val nextState = appState.copy(charts = merged)
            appState = nextState
            val textageRevision = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                store.save(nextState)
                store.setTextageSyncComplete(true)
                store.setTextageLastSyncAt(textageRevision)
            }
            textageProgress = TextageSyncProgress(
                initial = initial,
                completed = textageProgress?.total ?: imported.size,
                total = textageProgress?.total ?: imported.size,
                currentTitle = "歌曲元数据获取完成",
            )
            textageProgress = null
        } catch (error: Exception) {
            textageError = error.message ?: "Textage 更新失败"
            if (!initial) textageProgress = null
            throw error
        } finally {
            textageSyncing = false
        }
    }

    private suspend fun syncBjmMusicData() {
        val music = bjmClient.fetchMusicDatabase()
        if (music.isEmpty()) throw BjmException("BJM 曲目数据库为空")
        val nextState = appState.copy(bjmMusic = music)
        val revision = System.currentTimeMillis()
        appState = nextState
        withContext(Dispatchers.IO) {
            store.setBjmMusicRevision(revision)
            store.save(nextState)
        }
    }

    private suspend fun syncBjmScoresData(): Int {
        val result = bjmClient.fetchScores()
        val nextState = appState.copy(
            bjmScores = result.scores,
            bjmUser = result.user,
            bjmSyncedAt = System.currentTimeMillis(),
        )
        val revision = System.currentTimeMillis()
        appState = nextState
        withContext(Dispatchers.IO) {
            store.setBjmScoresRevision(revision)
            store.save(nextState)
        }
        return result.scores.size
    }

    private suspend fun rebuildFullBjmIndex() {
        val next = withContext(Dispatchers.Default) {
            buildBjmIndex(
                appState,
                store.textageLastSyncAt(),
                store.bjmMusicRevision(),
                store.bjmScoresRevision(),
            )
        }
        bjmIndex = next
        withContext(Dispatchers.IO) { store.saveBjmIndex(next) }
    }

    private suspend fun rebuildTextageBjmIndex() {
        val next = withContext(Dispatchers.Default) {
            rebuildBjmTextageIndex(
                appState,
                bjmIndex,
                store.textageLastSyncAt(),
                store.bjmMusicRevision(),
                store.bjmScoresRevision(),
            )
        }
        bjmIndex = next
        withContext(Dispatchers.IO) { store.saveBjmIndex(next) }
    }

    private suspend fun rebuildBjmMusicIndex() {
        val next = withContext(Dispatchers.Default) {
            rebuildBjmMusicIndex(
                appState,
                bjmIndex,
                store.textageLastSyncAt(),
                store.bjmMusicRevision(),
                store.bjmScoresRevision(),
            )
        }
        bjmIndex = next
        withContext(Dispatchers.IO) { store.saveBjmIndex(next) }
    }

    private suspend fun rebuildBjmScoresIndex() {
        val next = withContext(Dispatchers.Default) {
            rebuildBjmScoresIndex(
                appState,
                bjmIndex,
                store.textageLastSyncAt(),
                store.bjmMusicRevision(),
                store.bjmScoresRevision(),
            )
        }
        bjmIndex = next
        withContext(Dispatchers.IO) { store.saveBjmIndex(next) }
    }

    private fun openChart(chart: IidxChart) {
        selectedChart = chart
        selectedChartData = null
        chartLoading = true

        lifecycleScope.launch {
            try {
                val cached = withContext(Dispatchers.IO) { store.loadChartData(chart) }
                val usableCached = cached?.takeIf { cachedData ->
                    cachedData.parsed && (chart.notes <= 0 || cachedData.chart.notes == chart.notes)
                }
                val data = usableCached ?: run {
                    if (chart.textageUrl == null) throw TextageException("该谱面没有可用的 Textage 链接")
                    val fetched = textageClient.fetchChart(chart)
                    withContext(Dispatchers.IO) { store.saveChartData(fetched) }
                    fetched
                }
                if (selectedChart?.id == chart.id) selectedChartData = data
            } catch (error: Exception) {
                if (selectedChart?.id == chart.id) {
                    selectedChartData = null
                    message = error.message ?: "谱面数据获取失败"
                }
            } finally {
                if (selectedChart?.id == chart.id) chartLoading = false
            }
        }
    }

    private fun openSong(chart: IidxChart) {
        selectedSong = chart
        selectedChart = null
        selectedChartData = null
        chartLoading = false
    }

    private fun closeChart() {
        selectedChart = null
        selectedChartData = null
        chartLoading = false
    }

    private fun closeSong() {
        closeChart()
        selectedSong = null
    }

    private fun handleBack() {
        if (selectedChart != null) closeChart() else if (selectedSong != null) closeSong() else requestExit()
    }

    private fun requestExit() {
        if (exitToastShown) {
            finishAndRemoveTask()
            return
        }
        exitToastShown = true
        message = "再返回一次以退出"
        lifecycleScope.launch {
            delay(2_200L)
            exitToastShown = false
        }
    }

    private fun savePlayerSettings(settings: PlayerSettings) {
        playerSettings = settings.copy(speed = settings.safeSpeed)
        store.savePlayerSettings(playerSettings)
    }

    private fun clearChartCache() {
        lifecycleScope.launch(Dispatchers.IO) {
            store.clearChartCache()
            withContext(Dispatchers.Main) {
                selectedChartData = null
                message = "已清除谱面缓存"
            }
        }
    }
}

@Composable
private fun IidxApp(
    state: IidxAppState,
    bjmIndex: BjmIndex,
    localCatalogPresent: Boolean,
    textageSyncing: Boolean,
    textageProgress: TextageSyncProgress?,
    textageError: String?,
    selectedSong: IidxChart?,
    selectedChart: IidxChart?,
    chartData: TextageChartData?,
    chartLoading: Boolean,
    playerSettings: PlayerSettings,
    message: String?,
    onDismissMessage: () -> Unit,
    autoUpdateEnabled: Boolean,
    updateChecking: Boolean,
    updateInfo: GithubReleaseInfo?,
    updateDownloadProgress: Float?,
    updateInstalling: Boolean,
    syncTarget: DataSyncTarget?,
    syncStage: String?,
    settingsPageVisible: Boolean,
    onLogin: () -> Unit,
    onOpenBjmProfile: () -> Unit,
    onRefreshTextage: () -> Unit,
    onFullDataSync: () -> Unit,
    onSyncTextage: () -> Unit,
    onSyncBjmMusic: () -> Unit,
    onSyncBjmScores: () -> Unit,
    onOpenGithub: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onAutoUpdateEnabledChange: (Boolean) -> Unit,
    onClearChartCache: () -> Unit,
    onDismissUpdate: () -> Unit,
    onDownloadUpdate: (GithubReleaseInfo) -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    onOpenSong: (IidxChart) -> Unit,
    onCopyText: (String) -> Unit,
    onBack: () -> Unit,
    onRequestExit: () -> Unit,
    onRetryChart: () -> Unit,
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
) {
    Scaffold(containerColor = Background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            var browserMode by rememberSaveable { mutableStateOf("SP") }
            val chartsBySongKey = remember(state.charts) { state.charts.groupBy(::songGroupKey) }
            val showingBootstrap = !localCatalogPresent && (state.charts.isEmpty() || textageProgress?.initial == true)
            if (showingBootstrap) {
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
                    bjmIndex = bjmIndex,
                    mode = browserMode,
                    onModeChange = { browserMode = it },
                    textageSyncing = textageSyncing,
                    textageProgress = textageProgress,
                    textageError = textageError,
                    onLogin = onLogin,
                    onOpenBjmProfile = onOpenBjmProfile,
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
                    settingsPageVisible = settingsPageVisible,
                    onOpenSettings = onOpenSettings,
                    onDismissSettings = onDismissSettings,
                    autoUpdateEnabled = autoUpdateEnabled,
                    onAutoUpdateEnabledChange = onAutoUpdateEnabledChange,
                    onClearChartCache = onClearChartCache,
                    onOpenChart = onOpenChart,
                    onOpenSong = onOpenSong,
                    onCopyText = onCopyText,
                    showingDetail = selectedSong != null || selectedChart != null,
                    onBack = onBack,
                    onRequestExit = onRequestExit,
                    modifier = Modifier.alpha(if (selectedSong == null && selectedChart == null) 1f else 0f),
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
                        chartData = chartData,
                        loading = chartLoading,
                        playerSettings = playerSettings,
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
                        onPlayerSettingsChange = onPlayerSettingsChange,
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

@Composable
private fun ChartBrowserScreen(
    state: IidxAppState,
    bjmIndex: BjmIndex,
    mode: String,
    onModeChange: (String) -> Unit,
    textageSyncing: Boolean,
    textageProgress: TextageSyncProgress?,
    textageError: String?,
    onLogin: () -> Unit,
    onOpenBjmProfile: () -> Unit,
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
    settingsPageVisible: Boolean,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
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
                    Column(Modifier.weight(1f)) {
                        NavigationDrawerItem(
                            label = { Text("曲目列表") },
                            selected = !settingsPageVisible,
                            onClick = {
                                closeDrawer()
                                onDismissSettings()
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        NavigationDrawerItem(
                            label = { Text("设置") },
                            selected = settingsPageVisible,
                            onClick = {
                                closeDrawer()
                                onOpenSettings()
                            },
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
                syncTarget = syncTarget,
                syncStage = syncStage,
                bjmLoggedIn = state.bjmUser != null,
                onFullDataSync = onFullDataSync,
                onSyncTextage = onSyncTextage,
                onSyncBjmMusic = onSyncBjmMusic,
                onSyncBjmScores = onSyncBjmScores,
                onClearChartCache = onClearChartCache,
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
        val allSongs = remember(state.charts, mode) {
            state.charts
                .asSequence()
                .filter { it.mode == mode }
                .groupBy(::songGroupKey)
                .values
                .map { group ->
                    SongGroup(
                        key = songGroupKey(group.first()),
                        title = group.first().title,
                        subtitle = group.first().subtitle,
                        genre = group.first().genre,
                        composer = group.first().composer,
                        version = group.first().version,
                        sourceLabel = group.first().sourceLabel,
                        charts = group,
                    )
                }
        }
        val songs = remember(allSongs, query, selectedVersion, selectedLevel) {
            allSongs.mapNotNull { song ->
                val matchingCharts = song.charts.filter {
                    (selectedVersion == null || it.version == selectedVersion) &&
                        (selectedLevel == null || it.level == selectedLevel)
                }
                if (matchingCharts.isEmpty()) {
                    null
                } else if (
                    query.isNotBlank() &&
                    !"${song.title} ${song.subtitle} ${song.genre} ${song.composer}".contains(query, ignoreCase = true)
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
        Column(modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { drawerScope.launch { drawerState.open() } },
                    modifier = Modifier.size(42.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("☰", color = Ink, fontSize = 24.sp) }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("曲目列表", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("Style：", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(5.dp))
                    OutlinedButton(
                        onClick = { onModeChange(if (mode == "SP") "DP" else "SP") },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Text(mode, color = Purple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                val avatarText = state.bjmUser
                    ?.let { (it.name.ifBlank { it.id }).firstOrNull()?.toString()?.uppercase() }
                    ?: "○"
                Box(
                    Modifier.size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (state.bjmUser == null) Panel else Purple.copy(alpha = .18f))
                        .clickable { if (state.bjmUser == null) onLogin() else onOpenBjmProfile() },
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
                    placeholder = { Text("搜索曲名或艺术家", color = Muted) },
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
                    FunnelIcon(if (filterExpanded || selectedVersion != null || selectedLevel != null) Purple else Muted)
                }
            }
            if (filterExpanded) {
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
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) { Text("清除", color = Muted, fontSize = 11.sp) }
                }
            }
            val activeFilterSummary = buildString {
                selectedVersion?.let { append(it) }
                selectedLevel?.let {
                    if (isNotEmpty()) append("，")
                    append("LEVEL $it")
                }
            }
            if (!filterExpanded && activeFilterSummary.isNotBlank()) {
                Text(
                    "已筛选：$activeFilterSummary",
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
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
private fun UpdateSettingsScreen(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenMenu: () -> Unit,
    textageProgress: TextageSyncProgress?,
    textageError: String?,
    syncTarget: DataSyncTarget?,
    syncStage: String?,
    bjmLoggedIn: Boolean,
    onFullDataSync: () -> Unit,
    onSyncTextage: () -> Unit,
    onSyncBjmMusic: () -> Unit,
    onSyncBjmScores: () -> Unit,
    onClearChartCache: () -> Unit,
) {
    val syncing = syncTarget != null

    Column(Modifier.fillMaxSize().background(Background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onOpenMenu,
                modifier = Modifier.size(42.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Text("☰", color = Ink, fontSize = 24.sp)
            }
            Text("设置", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = ComposeColor(0xFFE5E3EC))
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("自动检查更新", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("每天检查 GitHub Release 是否有新版本", color = Muted, fontSize = 12.sp)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            HorizontalDivider(color = ComposeColor(0xFFE5E3EC))
            SettingsActionRow(
                title = "全量数据同步",
                subtitle = "按顺序同步 Textage、BJM 曲目库和用户成绩",
                actionLabel = if (syncTarget == DataSyncTarget.FULL) "同步中…" else "同步",
                enabled = !syncing,
                onClick = onFullDataSync,
            )
            if (!syncStage.isNullOrBlank()) {
                Text(
                    syncStage,
                    color = Purple,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                )
            }
            HorizontalDivider(color = ComposeColor(0xFFE5E3EC))
            Text(
                "单独同步数据源",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 2.dp),
            )
            SettingsActionRow(
                title = "Textage 歌曲库",
                subtitle = "同步歌曲元数据",
                actionLabel = if (syncTarget == DataSyncTarget.TEXTAGE) "同步中…" else "同步",
                enabled = !syncing,
                onClick = onSyncTextage,
            )
            SettingsActionRow(
                title = "BJM 曲目库",
                subtitle = "同步 BJM 曲目元数据",
                actionLabel = when {
                    syncTarget == DataSyncTarget.BJM_MUSIC -> "同步中…"
                    !bjmLoggedIn -> "需登录"
                    else -> "同步"
                },
                enabled = !syncing && bjmLoggedIn,
                onClick = onSyncBjmMusic,
            )
            SettingsActionRow(
                title = "用户成绩库",
                subtitle = "同步当前 BJM 用户成绩",
                actionLabel = when {
                    syncTarget == DataSyncTarget.BJM_SCORES -> "同步中…"
                    !bjmLoggedIn -> "需登录"
                    else -> "同步"
                },
                enabled = !syncing && bjmLoggedIn,
                onClick = onSyncBjmScores,
            )
            SettingsActionRow(
                title = "清除谱面缓存",
                subtitle = "下次打开谱面时重新解析",
                actionLabel = "清除",
                enabled = !syncing,
                onClick = onClearChartCache,
            )
            if (textageProgress != null) TextageSyncBanner(textageProgress)
            if (!textageError.isNullOrBlank()) {
                Text(
                    textageError,
                    color = Orange,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        TextButton(onClick = onClick, enabled = enabled) {
            Text(actionLabel, color = if (enabled) Purple else Muted)
        }
    }
}

@Composable
private fun UpdateDialog(
    release: GithubReleaseInfo,
    downloadProgress: Float?,
    installing: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    val busy = downloadProgress != null || installing
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("发现新版本") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "v${BuildConfig.VERSION_NAME.removePrefix("v")} → v${release.tagName.removePrefix("v")}",
                    color = Purple,
                    fontWeight = FontWeight.Bold,
                )
                Text(release.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(release.notes.ifBlank { "暂无 Release Note" }, color = Muted, fontSize = 12.sp)
                if (downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        if (downloadProgress <= 0f) "正在准备下载…" else "正在下载 ${(downloadProgress * 100).toInt()}%",
                        color = Muted,
                        fontSize = 11.sp,
                    )
                } else if (installing) {
                    Text("正在启动安装…", color = Muted, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload, enabled = !busy) {
                Text(if (installing) "安装中…" else "更新")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("稍后") }
        },
    )
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

private fun versionNumber(value: String): Int =
    Regex("\\d+").find(value)?.value?.toIntOrNull() ?: Int.MAX_VALUE

private fun textageVersionIndex(url: String): Int? =
    Regex("/score/([^/]+)/").find(url)?.groupValues?.getOrNull(1)?.let { directory ->
        if (directory == "s") 35 else directory.toIntOrNull()
    }

internal fun chartSongKey(chart: IidxChart): String =
    chart.textageUrl
        ?.substringBefore('?')
        ?.substringAfterLast('/')
        ?.substringBeforeLast('.')
        ?.takeIf { it.isNotBlank() }
        ?: chart.id.substringBeforeLast('-').removePrefix("textage-")

internal fun songGroupKey(chart: IidxChart): String = listOf(
    chart.title,
    chart.subtitle,
    chart.genre,
    chart.composer,
    chart.sourceLabel,
).joinToString("\u0000", transform = ::normalizeMusicTitle)

private fun displayTitle(title: String, sourceLabel: String): String =
    listOf(title.trim(), sourceLabel.trim()).filter { it.isNotBlank() }.joinToString(" ")

private data class SongGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val genre: String,
    val composer: String,
    val version: String,
    val sourceLabel: String,
    val charts: List<IidxChart>,
)

private fun difficultyOrder(value: String): Int = when (value) {
    "B" -> 0
    "N" -> 1
    "H" -> 2
    "A" -> 3
    "L" -> 4
    else -> 9
}

@Composable
private fun TextageBootstrapScreen(
    progress: TextageSyncProgress?,
    error: String?,
    retryEnabled: Boolean,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (error == null) "正在加载谱面元数据" else "谱面元数据获取失败", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            if (error == null) "首次启动会从 Textage 获取歌曲元数据并保存到本机。" else error,
            color = if (error == null) Muted else Orange,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(22.dp))

        if (progress == null) {
            CircularProgressIndicator(color = Purple)
        } else {
            Text(
                if (progress.total == 0) "正在检查本地谱面缓存…" else "正在获取：${progress.currentTitle}",
                color = Cyan,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).background(ComposeColor(0xFF26283B))) {
                Box(
                    Modifier.fillMaxWidth(progress.fraction).height(8.dp)
                        .background(ComposeColor(0xFFA88CFF)),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("${progress.completed} / ${progress.total}", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (progress.failed > 0) {
                Spacer(Modifier.height(5.dp))
                Text("失败 ${progress.failed} 张，将在下次启动继续重试", color = Orange, fontSize = 11.sp)
            }
        }

        if (error != null) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onRetry, enabled = retryEnabled) { Text(if (retryEnabled) "重试" else "重试中") }
        }
    }
}

@Composable
private fun TextageSyncBanner(progress: TextageSyncProgress) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("正在同步谱面数据", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${progress.completed}/${progress.total}", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)).background(ComposeColor(0xFF26283B))) {
            Box(Modifier.fillMaxWidth(progress.fraction).height(4.dp).background(Purple))
        }
        Text(
            "当前：${progress.currentTitle}${if (progress.failed > 0) " · 失败 ${progress.failed} 张" else ""}",
            color = Muted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun SongGroupRow(
    song: SongGroup,
    bjmIndex: BjmIndex,
    onOpenSong: (IidxChart) -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val representative = song.charts.firstOrNull()
    Column(
        modifier.fillMaxWidth()
            .border(1.dp, ComposeColor(0xFF292B42), RoundedCornerShape(12.dp))
            .clickable(enabled = representative != null) { representative?.let(onOpenSong) }
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                CopyableText(
                    text = song.genre.ifBlank { "未知曲风" },
                    color = Muted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onCopy = onCopyText,
                )
                Spacer(Modifier.height(4.dp))
                Text(displayTitle(song.title, song.sourceLabel), color = Ink, fontSize = 16.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (song.subtitle.isNotBlank()) {
                    Text(song.subtitle, color = Muted, fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    song.composer.ifBlank { "未知曲师" },
                    color = Muted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(song.version.ifBlank { "—" }, color = NormalBlue, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Muted)) { append("BPM ") }
                        withStyle(SpanStyle(color = NormalBlue)) {
                            append(song.charts.firstOrNull()?.bpm?.ifBlank { "—" } ?: "—")
                        }
                    },
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            song.charts.forEach { chart ->
                DifficultyChip(
                    chart = chart,
                    onOpenChart = onOpenChart,
                    score = scoreForChart(chart, bjmIndex),
                )
            }
        }
    }
}

@Composable
private fun DifficultyChip(
    chart: IidxChart,
    onOpenChart: (IidxChart) -> Unit,
    selected: Boolean = false,
    score: BjmScore? = null,
) {
    val accent = difficultyColor(chart.difficulty)
    val shape = RoundedCornerShape(14.dp)
    val available = chart.textageUrl != null
    Box(
        Modifier.size(width = 42.dp, height = 34.dp)
            .clip(shape)
            .background(if (available) accent.copy(alpha = .13f) else Background)
            .then(
                if (available) {
                    Modifier.border(
                        if (selected) 2.dp else 1.dp,
                        accent.copy(alpha = if (selected) .95f else .55f),
                        shape,
                    )
                } else {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = Muted.copy(alpha = .7f),
                            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 4.dp.toPx()))),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                        )
                    }
                },
            )
            .clickable(enabled = available) { onOpenChart(chart) }
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (score == null) {
            Text(if (chart.level > 0) chart.level.toString() else "—", color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        } else {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(if (chart.level > 0) chart.level.toString() else "—", color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        clearFlagShortName(score.clearFlag),
                        color = clearFlagColor(score.clearFlag),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        scoreRankName(score.exScore, chart.notes),
                        color = Ink,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun difficultyColor(value: String): ComposeColor = when (value) {
    "B" -> Green
    "N" -> NormalBlue
    "H" -> ComposeColor(0xFFFFD37A)
    "A" -> ComposeColor(0xFFD04C77)
    "L" -> ComposeColor(0xFF5635B8)
    else -> Muted
}

private fun scoreForChart(chart: IidxChart, index: BjmIndex): BjmScore? {
    val musicId = index.songMusicIds[songGroupKey(chart)] ?: return null
    return index.scoresByKey["$musicId:${if (chart.mode == "DP") 1 else 0}:${difficultyIndex(chart.difficulty)}"]
}

private fun clearFlagShortName(value: Int): String = when (value) {
    1 -> "F"
    2 -> "AC"
    3 -> "EC"
    4 -> "NC"
    5 -> "HC"
    6 -> "EXC"
    7 -> "FC"
    else -> ""
}

private fun clearFlagDetailName(value: Int): String = when (value) {
    1 -> "FAILED"
    2 -> "AC-CLEAR"
    3 -> "EC-CLEAR"
    4 -> "NC-CLEAR"
    5 -> "HC-CLEAR"
    6 -> "EXH-CLEAR"
    7 -> "FULL COMBO"
    else -> "NO PLAY"
}

private fun clearFlagColor(value: Int): ComposeColor = when (value) {
    1 -> ComposeColor(0xFFE05252)
    2 -> ComposeColor(0xFFE59B3C)
    3 -> ComposeColor(0xFF4AAE68)
    4 -> Cyan
    5 -> NormalBlue
    6 -> Purple
    7 -> ComposeColor(0xFFB68A00)
    else -> Muted
}

private fun scoreRankName(exScore: Int, noteCount: Int): String =
    rankSummary(exScore, noteCount).substringBefore(' ').takeIf { it != "—" }.orEmpty()

private fun rankDeltaText(exScore: Int, noteCount: Int): String =
    rankSummary(exScore, noteCount).substringAfter(' ', "").trim()

@Composable
private fun SongDetailScreen(
    song: IidxChart,
    charts: List<IidxChart>,
    bjmIndex: BjmIndex,
    mode: String,
    onBack: () -> Unit,
    onStyleToggle: () -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    onCopyText: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(width = 64.dp, height = 32.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "〈返回",
                    style = TextStyle(
                        color = Purple,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                )
            }
            Text("曲目信息", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Style：", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            OutlinedButton(
                onClick = onStyleToggle,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp),
            ) { Text(mode, color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                AutoScrollingText(song.genre.ifBlank { "未知曲风" }, color = Muted, fontSize = 10.sp, onLongPress = { onCopyText(song.genre) })
                Spacer(Modifier.height(3.dp))
                AutoScrollingText(displayTitle(song.title, song.sourceLabel), color = Ink, fontSize = 27.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, onLongPress = { onCopyText(song.title) })
                if (song.subtitle.isNotBlank()) {
                    AutoScrollingText(song.subtitle, color = Muted, fontSize = 12.sp, lineHeight = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                AutoScrollingText(song.composer.ifBlank { "未知曲师" }, color = Muted, fontSize = 13.sp, onLongPress = { onCopyText(song.composer) })
            }
            Column(horizontalAlignment = Alignment.End) {
                DetailStat("版本 ", song.version.ifBlank { "—" })
                DetailStat("BPM ", song.bpm.ifBlank { "—" })
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(charts, key = { it.id }) { chart ->
                DifficultyScoreCard(
                    chart = chart,
                    score = scoreForChart(chart, bjmIndex),
                    onOpenChart = onOpenChart,
                )
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

@Composable
private fun DifficultyScoreCard(
    chart: IidxChart,
    score: BjmScore?,
    onOpenChart: (IidxChart) -> Unit,
) {
    val accent = difficultyColor(chart.difficulty)
    val shape = RoundedCornerShape(10.dp)
    val available = chart.textageUrl != null
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(if (available) accent.copy(alpha = .08f) else Background)
            .border(1.dp, accent.copy(alpha = if (available) .65f else .3f), shape)
            .clickable(enabled = available) { onOpenChart(chart) }
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .heightIn(min = 72.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${difficultyName(chart.difficulty)} ${chart.level}",
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            if (score == null) {
                Text("NO PLAY", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StrokedText(
                        text = clearFlagDetailName(score.clearFlag),
                        fillColor = clearFlagColor(score.clearFlag),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    score.missCount.takeIf { it >= 0 }?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(" (", color = Muted, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text(it.toString(), color = NormalBlue, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text(" BP)", color = Muted, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(1.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${chart.notes.takeIf { it > 0 } ?: "—"} NOTES",
                color = Muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            if (score != null) {
                Text(score.exScore.toString(), color = NormalBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text("(", color = Muted, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                StrokedText(
                    text = scoreRankName(score.exScore, chart.notes),
                    fillColor = ComposeColor.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    strokeWidth = 1.3f,
                )
                rankDeltaText(score.exScore, chart.notes)
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        Spacer(Modifier.width(3.dp))
                        Text(it, color = Muted, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                Text(")", color = Muted, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun difficultyIndex(value: String): Int = when (value) {
    "B" -> 0
    "N" -> 1
    "H" -> 2
    "A" -> 3
    "L" -> 4
    else -> -1
}.coerceAtLeast(0)

private fun rankSummary(exScore: Int, noteCount: Int): String {
    if (noteCount <= 0) return "—"
    val thresholds = (1..8).map { step -> kotlin.math.ceil(noteCount * 2.0 * step / 9.0).toInt() }
    val rankIndex = thresholds.indexOfLast { exScore >= it }
    val rankNames = listOf("F", "E", "D", "C", "B", "A", "AA", "AAA")
    val currentThreshold = thresholds.getOrElse(rankIndex) { 0 }
    val rankPlus = exScore - currentThreshold
    val nextIndex = rankIndex + 1
    val nextThreshold = thresholds.getOrNull(nextIndex)
    if (nextThreshold == null) return "AAA + $rankPlus"
    val nextMinus = nextThreshold - exScore
    return if (rankIndex < 0 || nextMinus < rankPlus) {
        "${rankNames.getOrElse(nextIndex) { "AAA" }} - $nextMinus"
    } else {
        "${rankNames.getOrElse(rankIndex) { "F" }} + $rankPlus"
    }
}

private fun normalizeMusicTitle(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]"), "")

private fun buildBjmMusicIndex(music: List<BjmMusic>): Map<String, List<BjmMusic>> =
    music
        .flatMap { candidate ->
            setOf(candidate.title, candidate.plainTitle)
                .map(::normalizeMusicTitle)
                .filter(String::isNotBlank)
                .map { it to candidate }
        }
        .groupBy({ it.first }, { it.second })

private fun findBjmMusic(chart: IidxChart, index: Map<String, List<BjmMusic>>): BjmMusic? {
    val titleKey = normalizeMusicTitle(chart.title)
    val candidates = index[titleKey].orEmpty()
    if (candidates.isEmpty()) return null
    val chartVersion = chart.textageUrl?.let(::textageVersionIndex)
    return candidates.maxWithOrNull(
        compareBy<BjmMusic>(
            { it.level(chart.mode, chart.difficulty) == chart.level },
            { chartVersion != null && it.version == chartVersion },
            { it.version },
        ),
    )
}

private fun buildSongMusicIds(charts: List<IidxChart>, music: List<BjmMusic>): Map<String, Int> {
    val musicIndex = buildBjmMusicIndex(music)
    return charts
        .groupBy(::songGroupKey)
        .asSequence()
        .mapNotNull { (songKey, songCharts) ->
            val representative = songCharts.minWithOrNull(
                compareBy<IidxChart>({ it.textageUrl == null }, { it.level <= 0 }, { difficultyOrder(it.difficulty) }),
            ) ?: return@mapNotNull null
            findBjmMusic(representative, musicIndex)?.musicId?.let { musicId -> songKey to musicId }
        }
        .toMap()
}

private fun buildBjmIndex(
    state: IidxAppState,
    textageRevision: Long,
    musicRevision: Long,
    scoresRevision: Long,
): BjmIndex {
    return BjmIndex(
        songMusicIds = buildSongMusicIds(state.charts, state.bjmMusic),
        scoresByKey = state.bjmScores.associateBy { it.key },
        textageRevision = textageRevision,
        musicRevision = musicRevision,
        scoresRevision = scoresRevision,
        built = true,
    )
}

private fun rebuildBjmTextageIndex(
    state: IidxAppState,
    previous: BjmIndex,
    textageRevision: Long,
    musicRevision: Long,
    scoresRevision: Long,
): BjmIndex = previous.copy(
    songMusicIds = buildSongMusicIds(state.charts, state.bjmMusic),
    textageRevision = textageRevision,
    musicRevision = musicRevision,
    scoresRevision = scoresRevision,
    built = true,
)

private fun rebuildBjmMusicIndex(
    state: IidxAppState,
    previous: BjmIndex,
    textageRevision: Long,
    musicRevision: Long,
    scoresRevision: Long,
): BjmIndex = previous.copy(
    songMusicIds = buildSongMusicIds(state.charts, state.bjmMusic),
    textageRevision = textageRevision,
    musicRevision = musicRevision,
    scoresRevision = scoresRevision,
    built = true,
)

private fun rebuildBjmScoresIndex(
    state: IidxAppState,
    previous: BjmIndex,
    textageRevision: Long,
    musicRevision: Long,
    scoresRevision: Long,
): BjmIndex = previous.copy(
    scoresByKey = state.bjmScores.associateBy { it.key },
    textageRevision = textageRevision,
    musicRevision = musicRevision,
    scoresRevision = scoresRevision,
    built = true,
)

private fun refreshCachedBjmIndex(
    state: IidxAppState,
    cached: BjmIndex,
    store: IidxLocalStore,
): BjmIndex {
    val textageRevision = store.textageLastSyncAt()
    val musicRevision = store.bjmMusicRevision()
    val scoresRevision = store.bjmScoresRevision()
    var refreshed = cached
    if (!cached.built || cached.textageRevision != textageRevision || cached.musicRevision != musicRevision) {
        refreshed = refreshed.copy(
            songMusicIds = buildSongMusicIds(state.charts, state.bjmMusic),
            textageRevision = textageRevision,
            musicRevision = musicRevision,
            built = true,
        )
    }
    if (!cached.built || refreshed.scoresRevision != scoresRevision) {
        refreshed = refreshed.copy(
            scoresByKey = state.bjmScores.associateBy { it.key },
            scoresRevision = scoresRevision,
            built = true,
        )
    }
    return refreshed
}

private data class StrokedTextMetrics(
    val width: Float,
    val height: Float,
    val baseline: Float,
)

@Composable
private fun StrokedText(
    text: String,
    fillColor: ComposeColor,
    fontSize: TextUnit,
    fontWeight: FontWeight? = null,
    strokeColor: ComposeColor = Ink,
    strokeWidth: Float = 0.8f,
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { fontSize.toPx() }
    val metrics = remember(text, textSizePx, fontWeight) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            typeface = Typeface.create(
                Typeface.DEFAULT,
                if ((fontWeight?.weight ?: FontWeight.Normal.weight) >= FontWeight.Bold.weight) Typeface.BOLD else Typeface.NORMAL,
            )
        }
        val fontMetrics = paint.fontMetrics
        StrokedTextMetrics(
            width = paint.measureText(text),
            height = fontMetrics.bottom - fontMetrics.top,
            baseline = -fontMetrics.top,
        )
    }
    val strokePx = with(density) { strokeWidth.dp.toPx() }
    Canvas(
        Modifier
            .width(with(density) { (metrics.width + strokePx * 2f).toDp() })
            .height(with(density) { (metrics.height + strokePx * 2f).toDp() }),
    ) {
        drawIntoCanvas { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = textSizePx
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    if ((fontWeight?.weight ?: FontWeight.Normal.weight) >= FontWeight.Bold.weight) Typeface.BOLD else Typeface.NORMAL,
                )
                this.strokeWidth = strokePx
                this.strokeJoin = Paint.Join.ROUND
            }
            val x = strokePx
            val y = strokePx + metrics.baseline
            paint.style = Paint.Style.STROKE
            paint.color = strokeColor.toArgb()
            canvas.nativeCanvas.drawText(text, x, y, paint)
            paint.style = Paint.Style.FILL
            paint.color = fillColor.toArgb()
            canvas.nativeCanvas.drawText(text, x, y, paint)
        }
    }
}

@Composable
private fun ChartDetailScreen(
    chart: IidxChart,
    siblingCharts: List<IidxChart>,
    chartData: TextageChartData?,
    loading: Boolean,
    playerSettings: PlayerSettings,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    mode: String,
    onStyleToggle: () -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    onCopyText: (String) -> Unit,
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 32.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "〈返回",
                    style = TextStyle(
                        color = Purple,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                )
            }
            Text("谱面浏览", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Style：", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            OutlinedButton(
                onClick = onStyleToggle,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp),
            ) { Text(mode, color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                AutoScrollingText(chart.genre.ifBlank { "未知曲风" }, color = Muted, fontSize = 10.sp, onLongPress = { onCopyText(chart.genre) })
                Spacer(Modifier.height(3.dp))
                AutoScrollingText(displayTitle(chart.title, chart.sourceLabel), color = Ink, fontSize = 27.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, onLongPress = { onCopyText(chart.title) })
                if (chart.subtitle.isNotBlank()) {
                    AutoScrollingText(chart.subtitle, color = Muted, fontSize = 12.sp, lineHeight = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                AutoScrollingText(chart.composer.ifBlank { "未知曲师" }, color = Muted, fontSize = 13.sp, onLongPress = { onCopyText(chart.composer) })
            }
            Column(horizontalAlignment = Alignment.End) {
                DetailStat("版本 ", chart.version.ifBlank { "—" })
                DetailStat("BPM ", chartData?.chart?.bpm?.ifBlank { chart.bpm } ?: chart.bpm.ifBlank { "—" })
                DetailStat("NOTES ", (chartData?.chart?.notes ?: chart.notes).takeIf { it > 0 }?.toString() ?: "—")
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(7.dp))
            Text(
                "${chart.mode} ${difficultyName(chart.difficulty)} ${chart.level}${chart.score?.let { " · EX $it" } ?: ""}",
                color = difficultyColor(chart.difficulty),
                fontSize = 10.sp,
                letterSpacing = .8.sp,
            )
            Spacer(Modifier.height(7.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                siblingCharts.forEach { sibling ->
                    DifficultyChip(sibling, onOpenChart, selected = sibling.id == chart.id)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        when {
            loading -> Box(Modifier.fillMaxWidth().height(520.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Purple)
                    Spacer(Modifier.height(12.dp))
                    Text("正在获取并解析 Textage…", color = Muted, fontSize = 12.sp)
                }
            }
            chartData == null -> ChartLoadError(onRetry)
            chartData.notes.isEmpty() -> ChartParseWarning(chartData.parserMessage ?: "没有可显示的时序数据。", onRetry)
            else -> ChartPlayer(
                data = chartData,
                settings = playerSettings,
                onSettingsChange = onPlayerSettingsChange,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

@Composable
private fun ChartLoadError(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("谱面数据获取失败", color = Orange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("请检查网络后重试", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun ChartParseWarning(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("已获取页面，但暂未识别谱面节点", color = Orange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("重新解析") }
    }
}

@Composable
private fun PlayerConfigBox(
    settings: PlayerSettings,
    isSp: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSettingsChange: (PlayerSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val summary = buildString {
        append("Hi-Speed: ${settings.safeSpeed}x")
        if (isSp) {
            append(", ${settings.side}")
            settings.safePlayOption.optionAbbreviation()
                .takeIf { settings.safePlayOption != "NONE" }
                ?.let { append(" $it") }
        } else {
            val options = listOf(settings.safePlayOption1P, settings.safePlayOption2P)
                .map { it.optionAbbreviation() }
            if (options.any { it != "NON" }) append(", ${options.joinToString("/")}")
        }
    }
    var speedInput by remember(settings.safeSpeed) { mutableStateOf(settings.safeSpeed.toString()) }
    Column(
        modifier.fillMaxWidth()
            .clip(shape)
            .background(Panel)
            .border(1.dp, ComposeColor(0xFFD8D6E1), shape),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(if (expanded) "播放器配置" else summary, color = if (expanded) Ink else Muted, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            if (expanded) {
                Text("收起", color = Muted, fontSize = 10.sp)
                Spacer(Modifier.width(4.dp))
                Text("▼", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("设置", color = Muted, fontSize = 10.sp)
                Spacer(Modifier.width(4.dp))
                Text("▲", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (expanded) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Hi-Speed", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { onSettingsChange(settings.copy(speed = (settings.safeSpeed - 1).coerceAtLeast(1))) },
                    modifier = Modifier.size(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("−", color = Purple, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                BasicTextField(
                    value = speedInput,
                    onValueChange = { value ->
                        val digits = value.filter(Char::isDigit).take(3)
                        speedInput = digits
                        digits.toIntOrNull()?.let { next ->
                            onSettingsChange(settings.copy(speed = next.coerceIn(1, 100)))
                        }
                    },
                    modifier = Modifier.width(46.dp).height(34.dp)
                        .border(1.dp, ComposeColor(0xFFB7B4C3), RoundedCornerShape(5.dp))
                        .padding(horizontal = 4.dp),
                    textStyle = TextStyle(color = Ink, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            innerTextField()
                        }
                    },
                )
                TextButton(
                    onClick = { onSettingsChange(settings.copy(speed = (settings.safeSpeed + 1).coerceAtMost(100))) },
                    modifier = Modifier.size(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("+", color = Purple, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
            if (isSp) {
                PlayerSettingChoiceRow(
                    label = "位置",
                    choices = listOf("1P", "2P"),
                    selected = settings.side,
                    onSelect = { onSettingsChange(settings.copy(side = it)) },
                )
            }
            if (isSp) {
                PlayerSettingChoiceRow(
                    label = "选项",
                    choices = listOf("无", "MIRROR", "RANDOM"),
                    selected = when (settings.safePlayOption) {
                        "MIRROR" -> "MIRROR"
                        "RANDOM" -> "RANDOM"
                        else -> "无"
                    },
                    onSelect = { selected ->
                        onSettingsChange(settings.copy(playOption = if (selected == "无") "NONE" else selected))
                    },
                )
                if (settings.safePlayOption == "RANDOM") {
                    RandomMappingRow(
                        label = "",
                        mapping = if (settings.side == "1P") settings.safeRandomMapping1P else settings.safeRandomMapping2P,
                        onMappingChange = { mapping ->
                            onSettingsChange(
                                if (settings.side == "1P") settings.copy(randomMapping1P = mapping)
                                else settings.copy(randomMapping2P = mapping),
                            )
                        },
                    )
                }
            } else {
                PlayerSettingChoiceRow(
                    label = "1P",
                    choices = listOf("无", "MIRROR", "RANDOM"),
                    selected = when (settings.safePlayOption1P) {
                        "MIRROR" -> "MIRROR"
                        "RANDOM" -> "RANDOM"
                        else -> "无"
                    },
                    onSelect = { selected ->
                        onSettingsChange(settings.copy(playOption1P = if (selected == "无") "NONE" else selected))
                    },
                )
                if (settings.safePlayOption1P == "RANDOM") {
                    RandomMappingRow(
                        label = "",
                        mapping = settings.safeRandomMapping1P,
                        onMappingChange = { onSettingsChange(settings.copy(randomMapping1P = it)) },
                    )
                }
                PlayerSettingChoiceRow(
                    label = "2P",
                    choices = listOf("无", "MIRROR", "RANDOM"),
                    selected = when (settings.safePlayOption2P) {
                        "MIRROR" -> "MIRROR"
                        "RANDOM" -> "RANDOM"
                        else -> "无"
                    },
                    onSelect = { selected ->
                        onSettingsChange(settings.copy(playOption2P = if (selected == "无") "NONE" else selected))
                    },
                )
                if (settings.safePlayOption2P == "RANDOM") {
                    RandomMappingRow(
                        label = "",
                        mapping = settings.safeRandomMapping2P,
                        onMappingChange = { onSettingsChange(settings.copy(randomMapping2P = it)) },
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerSwitchSetting("小节线", settings.showBarLines) { onSettingsChange(settings.copy(showBarLines = it)) }
                PlayerSwitchSetting("小节序号", settings.showMeasureNumbers) { onSettingsChange(settings.copy(showMeasureNumbers = it)) }
                PlayerSwitchSetting("变速线", settings.showBpmChanges) { onSettingsChange(settings.copy(showBpmChanges = it)) }
            }
        }
    }
}

@Composable
private fun PlayerSettingChoiceRow(
    label: String,
    choices: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Muted, fontSize = 11.sp, modifier = Modifier.width(48.dp))
        choices.forEach { choice ->
            PlayerChoice(
                label = choice,
                selected = choice == selected,
                onClick = { onSelect(choice) },
                modifier = Modifier.padding(end = 6.dp),
            )
        }
    }
}

@Composable
private fun PlayerChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Purple.copy(alpha = .13f) else Background)
            .border(1.dp, if (selected) Purple else ComposeColor(0xFFCAC7D6), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Purple else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlayerSwitchSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 9.sp, maxLines = 1)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 1.dp).scale(.72f),
        )
    }
}

@Composable
private fun RandomMappingRow(
    label: String,
    mapping: List<Int>,
    onMappingChange: (List<Int>) -> Unit,
) {
    val dragThreshold = with(LocalDensity.current) { 30.dp.toPx() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(if (label.isBlank()) 46.dp else 32.dp)) {
                if (label.isNotBlank()) {
                    Text(label, color = Muted, fontSize = 10.sp, lineHeight = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("拖动调整", color = Muted, fontSize = if (label.isBlank()) 10.sp else 7.sp, lineHeight = if (label.isBlank()) 10.sp else 8.sp, maxLines = 1)
            }
            Spacer(Modifier.width(6.dp))
            mapping.forEachIndexed { index, value ->
                RandomLaneButton(
                    value = value,
                    dragThreshold = dragThreshold,
                    onDragSwap = { shift ->
                        val target = (index + shift).coerceIn(0, mapping.lastIndex)
                        if (target != index) {
                            val updated = mapping.toMutableList()
                            val moved = updated[index]
                            updated[index] = updated[target]
                            updated[target] = moved
                            onMappingChange(updated)
                        }
                    },
                    modifier = Modifier.padding(end = 2.dp),
                )
            }
            RandomLaneButton(
                value = null,
                dragThreshold = dragThreshold,
                onRandomize = { onMappingChange((1..7).shuffled()) },
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun RandomLaneButton(
    value: Int?,
    dragThreshold: Float,
    onDragSwap: (Int) -> Unit = {},
    onRandomize: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var dragDistance by remember { mutableStateOf(0f) }
    val buttonShape = RoundedCornerShape(5.dp)
    val labelTextSize = with(LocalDensity.current) { if (value == null) 9.sp.toPx() else 13.sp.toPx() }
    Box(
        modifier
            .graphicsLayer {
                if (value != null) {
                    translationX = dragDistance
                    alpha = if (dragDistance == 0f) 1f else .5f
                }
            }
            .zIndex(if (value != null && dragDistance != 0f) 1f else 0f)
            .size(28.dp)
            .clip(buttonShape)
            .background(if (value == null) Panel else if (value % 2 == 1) ComposeColor.White else ComposeColor(0xFF252535))
            .border(1.dp, ComposeColor(0xFF11131A), buttonShape)
            .then(
                if (value == null) Modifier.clickable(onClick = onRandomize)
                else Modifier.pointerInput(value, dragThreshold) {
                    detectDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onDragCancel = { dragDistance = 0f },
                        onDragEnd = {
                            val shift = kotlin.math.round(dragDistance / dragThreshold).toInt()
                            dragDistance = 0f
                            if (shift != 0) onDragSwap(shift)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount.x
                        },
                    )
                },
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = labelTextSize
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    color = if (value == null) Purple.toArgb() else ComposeColor(0xFFE33D4F).toArgb()
                    style = if (value == null) Paint.Style.FILL else Paint.Style.STROKE
                    strokeWidth = if (value == null) 0f else 2.2f
                }
                val baseline = (size.height - (paint.ascent() + paint.descent())) / 2f
                val text = value?.toString() ?: "随机"
                canvas.nativeCanvas.drawText(text, size.width / 2f, baseline, paint)
                if (value != null) {
                    paint.style = Paint.Style.FILL
                    paint.color = ComposeColor(0xFFFFD23F).toArgb()
                    canvas.nativeCanvas.drawText(text, size.width / 2f, baseline, paint)
                }
            }
        }
    }
}

private fun formatPlayerTime(seconds: Float): String {
    val total = seconds.coerceAtLeast(0f).toInt()
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

private fun formatPlayerBpm(bpm: Float): String {
    val rounded = bpm.toInt()
    return if (kotlin.math.abs(bpm - rounded) < 0.01f) {
        rounded.toString()
    } else {
        String.format(Locale.US, "%.1f", bpm)
    }
}

@Composable
private fun ChartPlayer(
    data: TextageChartData,
    settings: PlayerSettings,
    onSettingsChange: (PlayerSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var playing by remember(data.chart.id) { mutableStateOf(false) }
    var currentBeat by remember(data.chart.id) { mutableStateOf(0f) }
    var configExpanded by remember(data.chart.id) { mutableStateOf(false) }
    val safeSpeed = settings.safeSpeed
    val duration = data.durationBeats.coerceAtLeast(4f)
    val totalMeasures = data.measureCount().coerceAtLeast(1)
    val currentMeasure = data.measureAt(currentBeat).coerceIn(1, totalMeasures)
    val passedNotes = data.notes.sumOf { note ->
        if (note.holdBeats > 0f) {
            (if (note.beat <= currentBeat + 0.001f) 1 else 0) +
                (if (note.beat + note.holdBeats <= currentBeat + 0.001f) 1 else 0)
        } else if (note.beat <= currentBeat + 0.001f) {
            1
        } else {
            0
        }
    }
    val totalNotes = data.chart.notes.takeIf { it > 0 } ?: passedNotes.coerceAtLeast(data.notes.size)
    val currentSeconds = data.secondsAtBeat(currentBeat)
    val totalSeconds = data.secondsAtBeat(duration).coerceAtLeast(0.001f)
    val progress = (currentSeconds / totalSeconds).coerceIn(0f, 1f)
    val currentBpm = data.bpmAt(currentBeat)

    LaunchedEffect(data.chart.id, playing) {
        if (!playing) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            if (lastFrameNanos == 0L) {
                lastFrameNanos = frameNanos
                continue
            }
            val seconds = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, .25f)
            lastFrameNanos = frameNanos
            currentBeat = data.beatAtSeconds(data.secondsAtBeat(currentBeat) + seconds)
            if (currentBeat >= duration) {
                currentBeat = duration
                playing = false
            }
        }
    }

    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("谱面播放器", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Muted)) { append("NOTE ") }
                        withStyle(SpanStyle(color = NormalBlue)) { append("$passedNotes/$totalNotes") }
                        withStyle(SpanStyle(color = Muted)) { append(" · MEASURE ") }
                        withStyle(SpanStyle(color = NormalBlue)) { append("$currentMeasure/$totalMeasures") }
                    },
                    fontSize = 10.sp,
                )
            }
            TextButton(
                onClick = {
                    playing = false
                    val measureStart = data.measureStart(currentMeasure)
                    val targetMeasure = if (currentBeat <= measureStart + 0.001f) {
                        currentMeasure - 1
                    } else {
                        currentMeasure
                    }
                    currentBeat = data.measureStart(targetMeasure.coerceAtLeast(1))
                },
                modifier = Modifier.size(34.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Text("|‹", color = Purple, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            TextButton(
                onClick = {
                    if (!playing && currentBeat >= duration) currentBeat = 0f
                    playing = !playing
                },
                modifier = Modifier.size(42.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Text(if (playing) "Ⅱ" else "▶", color = Purple, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
            TextButton(
                onClick = {
                    playing = false
                    currentBeat = if (currentMeasure >= totalMeasures) {
                        duration
                    } else {
                        data.measureStart(currentMeasure + 1).coerceAtMost(duration)
                    }
                },
                modifier = Modifier.size(34.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Text("›|", color = Purple, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
        Box(Modifier.fillMaxWidth().height(18.dp)) {
            Text(
                formatPlayerTime(currentSeconds),
                color = Muted,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                "BPM ${formatPlayerBpm(currentBpm)}",
                color = NormalBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                formatPlayerTime(totalSeconds),
                color = Muted,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        Slider(
            value = progress,
            onValueChange = {
                playing = false
                currentBeat = data.beatAtSeconds(it * totalSeconds).coerceIn(0f, duration)
            },
            modifier = Modifier.fillMaxWidth().height(24.dp),
        )
        Spacer(Modifier.height(8.dp))

        Box(Modifier.fillMaxWidth().weight(1f)) {
            Box(
                Modifier.fillMaxWidth()
                    .height(360.dp)
                    .align(Alignment.TopCenter)
                    .background(PlayerBackground),
            ) {
                ChartCanvas(
                    data = data,
                    currentBeat = currentBeat,
                    speed = safeSpeed,
                    showBarLines = settings.showBarLines,
                    showBpmChanges = settings.showBpmChanges,
                    showMeasureNumbers = settings.showMeasureNumbers,
                    side = settings.side,
                    playOption = settings.safePlayOption,
                    playOption1P = settings.safePlayOption1P,
                    playOption2P = settings.safePlayOption2P,
                    randomMapping1P = settings.safeRandomMapping1P,
                    randomMapping2P = settings.safeRandomMapping2P,
                    playing = playing,
                    onCurrentBeatChange = { currentBeat = it.coerceIn(0f, duration) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            PlayerConfigBox(
                settings = settings,
                isSp = data.chart.mode != "DP",
                expanded = configExpanded,
                onExpandedChange = {
                    configExpanded = it
                    if (it) playing = false
                },
                onSettingsChange = { next -> onSettingsChange(next.copy(speed = next.safeSpeed)) },
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(3f),
            )
        }
        if (!data.parsed) Text(data.parserMessage ?: "当前谱面格式尚未完成解析。", color = Orange, fontSize = 10.sp)
    }
}

@Composable
private fun ChartCanvas(
    data: TextageChartData,
    currentBeat: Float,
    speed: Int,
    showBarLines: Boolean,
    showBpmChanges: Boolean,
    showMeasureNumbers: Boolean,
    side: String,
    playOption: String,
    playOption1P: String,
    playOption2P: String,
    randomMapping1P: List<Int>,
    randomMapping2P: List<Int>,
    playing: Boolean,
    onCurrentBeatChange: (Float) -> Unit,
    modifier: Modifier,
) {
    val laneCount = if (data.chart.mode == "DP") 16 else 8
    // Keep note geometry in musical beat coordinates. BPM changes are applied
    // by playback timing, which changes the visible scroll speed without
    // stretching the chart a second time and creating gaps between measures.
    // Hi-Speed 1 is four times the old visual baseline.
    val pixelsPerBeat = 16f * speed * 4f
    val labelTextSize = with(LocalDensity.current) { 10.sp.toPx() }
    val labelPadding = with(LocalDensity.current) { 4.dp.toPx() }
    val latestCurrentBeat by androidx.compose.runtime.rememberUpdatedState(currentBeat)
    val latestPlaying by androidx.compose.runtime.rememberUpdatedState(playing)
    val latestOnCurrentBeatChange by androidx.compose.runtime.rememberUpdatedState(onCurrentBeatChange)
    Canvas(
        modifier.pointerInput(data.chart.id, speed) {
            detectVerticalDragGestures { _, dragAmount ->
                if (!latestPlaying) {
                    latestOnCurrentBeatChange(latestCurrentBeat + dragAmount / pixelsPerBeat)
                }
            }
        },
    ) {
        val isSp = data.chart.mode != "DP"
        val dpGapUnits = 1.5f
        // SP has a gray information column opposite the scratch column. It
        // uses the same 1.5-note width as the DP center gap.
        val laneWidths = if (isSp) {
            listOf(1.5f) + List(7) { 1f } + listOf(1.5f)
        } else listOf(1.5f) + List(7) { 1f } + List(7) { 1f } + listOf(1.5f)
        val unit = if (isSp) size.width / laneWidths.sum() else size.width / (laneWidths.sum() + dpGapUnits)
        val laneLefts = laneWidths.runningFold(0f) { sum, width -> sum + width * unit }.dropLast(1)
        val spLaneOffset = if (isSp && side == "2P") 1 else 0
        val dpLeftWidth = laneWidths.take(8).sum()
        fun dpLaneStart(lane: Int): Float = if (lane < 8) {
            laneWidths.take(lane).sum() * unit
        } else {
            (dpLeftWidth + dpGapUnits + laneWidths.slice(8 until lane).sum()) * unit
        }
        fun dpBoundaryX(boundary: Int): Float = if (boundary <= 8) {
            laneWidths.take(boundary).sum() * unit
        } else {
            (dpLeftWidth + dpGapUnits + laneWidths.slice(8 until boundary).sum()) * unit
        }
        val judgeY = size.height * .92f

        drawRect(PlayerBackground)
        if (isSp) {
            val grayStart = if (side == "1P") laneWidths.take(8).sum() * unit else 0f
            drawRect(
                color = PlayerCenterGap,
                topLeft = Offset(grayStart, 0f),
                size = Size(1.5f * unit, size.height),
            )
        } else {
            drawRect(
                color = PlayerCenterGap,
                topLeft = Offset(dpLeftWidth * unit, 0f),
                size = Size(dpGapUnits * unit, size.height),
            )
        }
        if (isSp) {
            for (boundary in 0..laneWidths.size) {
                val x = laneLefts.getOrNull(boundary) ?: size.width
                drawLine(
                    color = if (boundary == 0 || boundary == laneWidths.size) PlayerEdge else PlayerLane,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (boundary == 0 || boundary == laneWidths.size) 2f else 1f,
                )
            }
        } else {
            for (lane in 0..laneCount) {
                val x = dpBoundaryX(lane)
                drawLine(
                    color = if (lane == 0 || lane == laneCount) PlayerEdge else PlayerLane,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (lane == 0 || lane == laneCount) 2f else 1f,
                )
            }
        }
        if (!isSp) {
            drawLine(
                color = PlayerEdge,
                start = Offset((dpLeftWidth + dpGapUnits) * unit, 0f),
                end = Offset((dpLeftWidth + dpGapUnits) * unit, size.height),
                strokeWidth = 2f,
            )
        }
        val infoStartX = if (isSp) {
            if (side == "1P") laneWidths.take(8).sum() * unit else 0f
        } else {
            dpLeftWidth * unit
        }
        val infoEndX = if (isSp) {
            infoStartX + 1.5f * unit
        } else {
            infoStartX + dpGapUnits * unit
        }
        val infoOnLeft = isSp && side == "2P"
        val measureX = if (infoOnLeft) infoEndX - labelPadding else infoStartX + labelPadding
        val measureAlign = if (infoOnLeft) Paint.Align.RIGHT else Paint.Align.LEFT
        val bpmX = if (infoOnLeft) infoStartX + labelPadding else infoEndX - labelPadding
        val bpmAlign = if (infoOnLeft) Paint.Align.LEFT else Paint.Align.RIGHT
        val measureBaseline = { y: Float -> (y - 4f).coerceAtLeast(labelTextSize) }
        val bpmBaseline = { y: Float -> (y - 4f).coerceAtLeast(labelTextSize) }
        if (showBarLines || showMeasureNumbers) {
            val firstMeasure = (
                data.measureAt(currentBeat - size.height / pixelsPerBeat) - 2
            ).coerceAtLeast(1)
            val lastMeasure = (
                data.measureAt(currentBeat + size.height / pixelsPerBeat) + 2
            ).coerceAtMost(data.measureCount())
            for (measure in firstMeasure..lastMeasure) {
                val measureBeat = data.measureStart(measure)
                val y = judgeY - (measureBeat - currentBeat) * pixelsPerBeat
                if (y in -2f..size.height + 2f) {
                    if (showBarLines) {
                        drawLine(
                            ComposeColor(0xFF444756),
                            Offset(0f, y),
                            Offset(size.width, y),
                            strokeWidth = if (measure % 4 == 1) 3f else 2f,
                        )
                    }
                    if (showMeasureNumbers) {
                        drawIntoCanvas { canvas ->
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = PlayerMeasureText.toArgb()
                                textSize = labelTextSize
                                typeface = Typeface.DEFAULT_BOLD
                                textAlign = measureAlign
                            }
                            canvas.nativeCanvas.drawText(
                                measure.toString(),
                                measureX,
                                measureBaseline(y),
                                paint,
                            )
                        }
                    }
                }
            }
        }
        if (showBpmChanges) {
            data.bpmChanges.filter { it.beat > 0f }.forEach { change ->
                val y = judgeY - (change.beat - currentBeat) * pixelsPerBeat
                if (y in -labelTextSize..size.height + labelTextSize) {
                    drawLine(
                        PlayerBpmGreen,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = 2f,
                    )
                    drawIntoCanvas { canvas ->
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = PlayerBpmGreen.toArgb()
                            textSize = labelTextSize
                            typeface = Typeface.DEFAULT_BOLD
                            textAlign = bpmAlign
                        }
                        canvas.nativeCanvas.drawText(
                            formatPlayerBpm(change.bpm),
                            bpmX,
                            bpmBaseline(y),
                            paint,
                        )
                    }
                }
            }
        }
        drawLine(PlayerRed, Offset(0f, judgeY), Offset(size.width, judgeY), strokeWidth = 5f)
        clipRect(0f, 0f, size.width, size.height) {
            data.notes.forEach { note ->
            val noteEndBeat = note.beat + note.holdBeats
            if (noteEndBeat < currentBeat - 0.001f) return@forEach
            val visibleStartBeat = maxOf(note.beat, currentBeat)
            val y = judgeY - (visibleStartBeat - currentBeat) * pixelsPerBeat
            val endY = judgeY - (noteEndBeat - currentBeat) * pixelsPerBeat
            if (maxOf(y, endY) < 0f || minOf(y, endY) > size.height) return@forEach
            // Keep the source lane separate from the displayed lane. RANDOM
            // changes only the position; note color must remain tied to the
            // original chart lane (especially scratch vs. key colors).
            val sourceLane = note.lane
            val rawLane = if (isSp) sourceLane.mod(8) else sourceLane.coerceIn(0, laneCount - 1)
            val laneOption = if (isSp) playOption else if (rawLane >= 8) playOption2P else playOption1P
            fun mappedKeyLane(lane: Int, mapping: List<Int>): Int = when (laneOption) {
                "MIRROR" -> 8 - lane
                "RANDOM" -> (mapping.indexOf(lane).takeIf { it >= 0 } ?: (lane - 1)) + 1
                else -> lane
            }
            val logicalLane = if (isSp && rawLane > 0) {
                mappedKeyLane(rawLane, if (side == "1P") randomMapping1P else randomMapping2P)
            } else rawLane
            val destinationKeyLane = when {
                isSp && rawLane > 0 -> logicalLane
                !isSp && rawLane in 1..7 -> mappedKeyLane(rawLane, randomMapping1P)
                !isSp && rawLane >= 9 -> mappedKeyLane(rawLane - 8, randomMapping2P)
                else -> 0
            }
            val displayLane = when {
                isSp && side == "2P" -> if (logicalLane == 0) 7 else logicalLane - 1
                !isSp && rawLane >= 8 -> dpDisplayLane(rawLane, destinationKeyLane)
                !isSp && rawLane in 1..7 -> destinationKeyLane
                else -> logicalLane
            }
            val laneIndex = displayLane.coerceIn(0, laneCount - 1)
            // In SP 2P the gray information column is physically before the
            // seven keys and scratch, so shift every displayed lane by one
            // slot. The logical lane mapping remains unchanged.
            val physicalLaneIndex = if (isSp) laneIndex + spLaneOffset else laneIndex
            val laneWidth = laneWidths.getOrElse(physicalLaneIndex) { 1f } * unit
            val laneStart = if (isSp) {
                laneLefts.getOrElse(physicalLaneIndex) { 0f }
            } else {
                dpLaneStart(laneIndex)
            }
            val left = laneStart + laneWidth * .12f
            val width = laneWidth * .76f
            // Colors follow the mapped physical key. For example, source
            // lane 6 moved to lane 7 becomes white after RANDOM.
            val noteColor = if (destinationKeyLane == 0) {
                PlayerRed
            } else if (destinationKeyLane % 2 == 1) {
                ComposeColor.White
            } else {
                PlayerSkyBlue
            }
            if (note.holdBeats > 0f) {
                val holdTop = minOf(y, endY).coerceIn(0f, size.height)
                val holdBottom = maxOf(y, endY).coerceIn(0f, size.height)
                val holdHeight = holdBottom - holdTop
                val holdWidth = width * .88f
                if (holdHeight > 0f) {
                    drawRect(
                        color = noteColor.copy(alpha = .58f),
                        topLeft = Offset(left + (width - holdWidth) / 2f, holdTop),
                        size = Size(holdWidth, holdHeight),
                    )
                }
            }
            if (note.beat >= currentBeat - 0.001f && y in 0f..size.height) {
                drawRoundRect(
                    color = noteColor,
                    topLeft = Offset(left, y - 6f),
                    size = Size(width, 12f),
                    cornerRadius = CornerRadius(5f),
                )
            }
            if (note.holdBeats > 0f && endY in 0f..size.height) {
                drawRoundRect(
                    color = noteColor,
                    topLeft = Offset(left, endY - 6f),
                    size = Size(width, 12f),
                    cornerRadius = CornerRadius(5f),
                )
            }
            }
        }
    }
}

/**
 * DP's 2P side is laid out as keys 1..7 from left to right, with the scratch
 * column on the far right. The source lane numbering already follows that
 * order, so it must not be mirrored when converting it to a display column.
 */
internal fun dpDisplayLane(rawLane: Int, destinationKeyLane: Int): Int = when {
    rawLane == 8 -> 15
    rawLane in 9..15 -> 8 + destinationKeyLane - 1
    else -> rawLane
}

@Composable
private fun CopyableText(
    text: String,
    color: ComposeColor,
    fontSize: TextUnit,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    onCopy: (String) -> Unit,
) {
    Text(
        text,
        color = color,
        fontSize = fontSize,
        maxLines = maxLines,
        overflow = overflow,
        modifier = Modifier.pointerInput(text) {
            detectTapGestures(onLongPress = { onCopy(text) })
        },
    )
}

@Composable
private fun AutoScrollingText(
    text: String,
    color: ComposeColor,
    fontSize: TextUnit,
    lineHeight: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(text, scrollState.maxValue) {
        if (scrollState.maxValue <= 0) return@LaunchedEffect
        scrollState.scrollTo(0)
        val durationMillis = (scrollState.maxValue * 7).coerceIn(1_200, 5_000)
        delay(900L)
        while (isActive) {
            scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
            )
            delay(900L)
            scrollState.animateScrollTo(
                0,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
            )
            delay(900L)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (onLongPress == null) {
                    Modifier
                } else {
                    Modifier.pointerInput(text) {
                        detectTapGestures(onLongPress = { onLongPress() })
                    }
                },
            )
            .horizontalScroll(scrollState, enabled = false),
    ) {
        Text(
            text,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun DetailValue(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Spacer(Modifier.width(4.dp))
        Text(value, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = Muted)) { append(label) }
            withStyle(SpanStyle(color = NormalBlue)) { append(value) }
        },
        fontSize = 10.sp,
        maxLines = 1,
        softWrap = false,
    )
}

private fun difficultyName(value: String): String = when (value) {
    "B" -> "BEGINNER"
    "N" -> "NORMAL"
    "H" -> "HYPER"
    "A" -> "ANOTHER"
    "L" -> "LEGGENDARIA"
    else -> value
}

private fun String.optionAbbreviation(): String = when (this) {
    "MIRROR" -> "MIR"
    "RANDOM" -> "RAN"
    else -> "NON"
}

@Composable
private fun ToastCard(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        delay(2_200L)
        onDismiss()
    }
    Surface(
        modifier.padding(14.dp).clickable { onDismiss() },
        color = ComposeColor(0xFF303038),
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 4.dp,
    ) {
        Text(message, color = ComposeColor.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
    }
}
