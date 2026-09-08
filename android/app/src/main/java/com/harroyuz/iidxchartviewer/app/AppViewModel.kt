package com.harroyuz.iidxchartviewer.app

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.harroyuz.iidxchartviewer.BuildConfig
import com.harroyuz.iidxchartviewer.data.local.IidxLocalStore
import com.harroyuz.iidxchartviewer.data.remote.bjm.BjmClient
import com.harroyuz.iidxchartviewer.data.remote.bjm.BjmException
import com.harroyuz.iidxchartviewer.data.remote.textage.TextageChartPage
import com.harroyuz.iidxchartviewer.data.remote.textage.TextageClient
import com.harroyuz.iidxchartviewer.data.remote.textage.TextageException
import com.harroyuz.iidxchartviewer.data.remote.update.GithubReleaseInfo
import com.harroyuz.iidxchartviewer.data.remote.update.GithubUpdateClient
import com.harroyuz.iidxchartviewer.domain.catalog.buildSongGroups
import com.harroyuz.iidxchartviewer.domain.catalog.songGroupKey
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxAppState
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.model.TextageChartData
import com.harroyuz.iidxchartviewer.domain.model.TextageSyncProgress
import com.harroyuz.iidxchartviewer.domain.player.PlayerSettings
import com.harroyuz.iidxchartviewer.domain.score.appendBjmHistory
import com.harroyuz.iidxchartviewer.domain.score.buildBjmIndex
import com.harroyuz.iidxchartviewer.domain.score.isPersistedBjmIndexUsable
import com.harroyuz.iidxchartviewer.domain.score.rebuildBjmCatalogIndex
import com.harroyuz.iidxchartviewer.domain.score.rebuildBjmScoresIndex
import com.harroyuz.iidxchartviewer.domain.sync.DataSyncTarget
import com.harroyuz.iidxchartviewer.domain.sync.UPDATE_CHECK_INTERVAL_MS
import com.harroyuz.iidxchartviewer.domain.sync.isSameLocalDate
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val eventsChannel = Channel<AppEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private val store = IidxLocalStore(application)
    private val bjmClient = BjmClient()
    private val textageClient = TextageClient()
    private val githubUpdateClient = GithubUpdateClient()

    internal var appState by mutableStateOf(IidxAppState())
        private set
    internal var bjmHistory by mutableStateOf<List<BjmScore>>(emptyList())
        private set
    internal var bjmIndex by mutableStateOf(BjmIndex())
        private set
    internal var localCatalogPresent by mutableStateOf(false)
        private set
    internal var localDataLoading by mutableStateOf(true)
        private set
    internal var localDataProgress by mutableStateOf(0f)
        private set
    internal var localDataStage by mutableStateOf("正在准备本地数据")
        private set
    internal var syncTarget by mutableStateOf<DataSyncTarget?>(null)
        private set
    internal var syncStage by mutableStateOf<String?>(null)
        private set
    internal var syncProgress by mutableStateOf<Float?>(null)
        private set
    private var syncProgressBase = 0f
    private var syncProgressWeight = 1f
    internal var textageSyncing by mutableStateOf(false)
        private set
    internal var textageProgress by mutableStateOf<TextageSyncProgress?>(null)
        private set
    internal var textageError by mutableStateOf<String?>(null)
        private set
    internal var textageLastSyncAt by mutableStateOf(0L)
        private set
    internal var bjmMusicLastSyncAt by mutableStateOf(0L)
        private set
    internal var bjmScoresLastSyncAt by mutableStateOf(0L)
        private set
    internal var chartLoading by mutableStateOf(false)
        private set
    internal var selectedSong by mutableStateOf<IidxChart?>(null)
        private set
    internal var selectedChart by mutableStateOf<IidxChart?>(null)
        private set
    internal var selectedChartData by mutableStateOf<TextageChartData?>(null)
        private set
    internal var spPlayerSettings by mutableStateOf(PlayerSettings())
        private set
    internal var dpPlayerSettings by mutableStateOf(PlayerSettings())
        private set
    internal var message by mutableStateOf<String?>(null)
        private set
    internal var autoUpdateEnabled by mutableStateOf(true)
        private set
    internal var updateChecking by mutableStateOf(false)
        private set
    internal var updateInfo by mutableStateOf<GithubReleaseInfo?>(null)
        private set
    internal var updateDownloadProgress by mutableStateOf<Float?>(null)
        private set
    internal var updateInstalling by mutableStateOf(false)
        private set
    private var destination by mutableStateOf(AppDestination.CATALOG)
    internal val settingsPageVisible: Boolean get() = destination == AppDestination.SETTINGS
    internal val bjmDataPageVisible: Boolean get() = destination == AppDestination.HISTORY
    private var returnToBjmHistory by mutableStateOf(false)
    private var exitToastShown = false

    init {
        localCatalogPresent = store.hasTextageCatalogMarker()
        autoUpdateEnabled = store.autoUpdateEnabled()
        textageLastSyncAt = store.textageLastSyncAt()
        bjmMusicLastSyncAt = store.bjmMusicRevision()
        bjmScoresLastSyncAt = store.bjmScoresRevision()
        spPlayerSettings = store.loadPlayerSettings("SP")
        dpPlayerSettings = store.loadPlayerSettings("DP")

        // Loading a large local catalog and its persisted index is kept off
        // the main thread. The progress screen prevents an empty list from
        // being mistaken for a failed load.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    localDataProgress = .08f
                    localDataStage = "正在读取本地曲目"
                }
                val loadedFromDisk = store.load()
                val loadedBjmHistory = store.loadBjmHistory()
                val authenticatedState = if (loadedFromDisk.bjmUser != null) {
                    val currentUser = runCatching {
                        bjmClient.probeAuthMe()
                    }.getOrNull()
                    if (currentUser == null) {
                        val loggedOut = loadedFromDisk.copy(bjmUser = null)
                        withContext(Dispatchers.Main) {
                            bjmClient.clearSession()
                        }
                        withContext(Dispatchers.IO) { store.save(loggedOut) }
                        withContext(Dispatchers.Main) {
                            message = "BJM 登录已失效，请重新登录"
                        }
                        loggedOut
                    } else {
                        loadedFromDisk.copy(bjmUser = currentUser)
                    }
                } else {
                    loadedFromDisk
                }
                withContext(Dispatchers.Main) {
                    appState = authenticatedState
                    bjmHistory = loadedBjmHistory
                    localDataProgress = .45f
                    localDataStage = if (
                        authenticatedState.charts.isNotEmpty() && authenticatedState.songGroups.isEmpty()
                    ) {
                        "正在构建本地曲目组"
                    } else {
                        "正在读取用户成绩索引"
                    }
                }
                val loaded = if (authenticatedState.charts.isNotEmpty() && authenticatedState.songGroups.isEmpty()) {
                    val migrated = authenticatedState.copy(
                        songGroups = withContext(Dispatchers.Default) {
                            buildSongGroups(authenticatedState.charts)
                        },
                    )
                    withContext(Dispatchers.IO) { store.save(migrated) }
                    withContext(Dispatchers.Main) {
                        appState = migrated
                        localDataProgress = .57f
                        localDataStage = "正在读取用户成绩索引"
                    }
                    migrated
                } else {
                    authenticatedState
                }
                val loadedBjmIndex = store.loadBjmIndex() ?: BjmIndex()
                val indexNeedsRebuild = !isPersistedBjmIndexUsable(loaded, loadedBjmIndex, store.textageLastSyncAt(), store.bjmMusicRevision(), store.bjmScoresRevision())
                withContext(Dispatchers.Main) {
                    bjmIndex = loadedBjmIndex
                    localDataProgress = if (indexNeedsRebuild) .62f else .92f
                    localDataStage = if (indexNeedsRebuild) "索引需要更新，正在重建" else "正在使用已保存索引"
                }
                val refreshedIndex = if (indexNeedsRebuild) {
                    try {
                        withContext(Dispatchers.Default) {
                            buildBjmIndex(
                                loaded,
                                store.textageLastSyncAt(),
                                store.bjmMusicRevision(),
                                store.bjmScoresRevision(),
                            )
                        }
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        withContext(Dispatchers.Main) {
                            message = "本地成绩索引重建失败，已使用旧索引"
                        }
                        loadedBjmIndex
                    }
                } else loadedBjmIndex
                withContext(Dispatchers.Main) {
                    if (
                        indexNeedsRebuild &&
                        appState.charts === loaded.charts &&
                        appState.bjmMusic === loaded.bjmMusic &&
                        appState.bjmScores === loaded.bjmScores
                    ) {
                        bjmIndex = refreshedIndex
                        withContext(Dispatchers.IO) { store.saveBjmIndex(refreshedIndex) }
                    }
                    localDataProgress = 1f
                    localDataStage = "本地数据加载完成"
                    localDataLoading = false
                    // Bootstrap only when there is no usable local catalog. A
                    // completed catalog should open immediately; the update
                    // button performs the next metadata refresh on demand.
                    val needsBootstrap = loaded.charts.isEmpty() || !store.isTextageSyncComplete()
                    val automaticSyncTargets = automaticDataSyncTargets()
                    when {
                        needsBootstrap -> syncAllData()
                        automaticSyncTargets.isNotEmpty() -> syncAllData(automatic = true)
                    }
                    checkForUpdatesIfDue()
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                withContext(Dispatchers.Main) {
                    localDataLoading = false
                    localDataStage = "本地数据加载失败"
                    message = error.message ?: "本地数据加载失败"
                }
            }
        }
    }

    internal fun dismissMessage() { message = null }
    internal fun showSettings(visible: Boolean) {
        if (visible) destination = AppDestination.SETTINGS
        else if (settingsPageVisible) destination = AppDestination.CATALOG
    }
    internal fun showBjmData(visible: Boolean) {
        if (visible) destination = AppDestination.HISTORY
        else if (bjmDataPageVisible) destination = AppDestination.CATALOG
    }
    internal fun changeAutoUpdate(enabled: Boolean) {
        autoUpdateEnabled = enabled
        store.setAutoUpdateEnabled(enabled)
    }
    internal fun dismissUpdate() {
        if (updateDownloadProgress == null && !updateInstalling) updateInfo = null
    }
    internal fun onPlatformActionFailed(error: Exception) {
        message = error.message ?: "无法打开系统应用"
    }

    fun onLoginCompleted() {
        message = "BJM 登录完成，正在同步成绩…"
        syncBjmScoresAfterLogin()
    }

    internal fun openBjmLogin() {
        eventsChannel.trySend(AppEvent.Login)
    }

    internal fun logoutBjm() {
        clearBjmSession("已退出 BJM")
    }

    private fun clearBjmSession(messageText: String) {
        bjmClient.clearSession()
        val loggedOut = appState.copy(bjmUser = null)
        appState = loggedOut
        message = messageText
        viewModelScope.launch(Dispatchers.IO) {
            store.save(loggedOut)
        }
    }

    internal fun openGithub() {
        eventsChannel.trySend(AppEvent.OpenProject)
    }

    internal fun copyText(value: String) {
        val text = value.trim()
        if (text.isBlank()) return
        val clipboard = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("IIDX Data", text))
        message = "已复制：$text"
    }

    private fun checkForUpdatesIfDue() {
        if (autoUpdateEnabled && System.currentTimeMillis() - store.updateLastCheckAt() >= UPDATE_CHECK_INTERVAL_MS) {
            checkForUpdates(manual = false)
        }
    }

    internal fun checkForUpdates(manual: Boolean) {
        if (updateChecking) return
        updateChecking = true
        viewModelScope.launch {
            try {
                val release = withContext(Dispatchers.IO) { githubUpdateClient.fetchLatestRelease() }
                store.setUpdateLastCheckAt()
                if (GithubUpdateClient.isNewer(BuildConfig.VERSION_NAME, release.tagName)) {
                    updateInfo = release
                } else if (manual) {
                    message = "当前已是最新版本 ${BuildConfig.VERSION_NAME}"
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (manual) message = error.message ?: "更新检查失败"
            } finally {
                updateChecking = false
            }
        }
    }

    internal fun downloadUpdate(release: GithubReleaseInfo) {
        if (updateDownloadProgress != null || updateInstalling) return
        updateDownloadProgress = 0f
        viewModelScope.launch {
            try {
                val apk = githubUpdateClient.downloadApk(getApplication<Application>(), release) { downloaded, total ->
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
                if (error is CancellationException) throw error
                message = error.message ?: "APK 下载失败"
            } finally {
                updateDownloadProgress = null
                updateInstalling = false
            }
        }
    }

    private fun installApk(apk: File) {
        eventsChannel.trySend(AppEvent.InstallApk(apk))
    }

    private fun startDataSync(target: DataSyncTarget, action: suspend () -> Unit) {
        if (syncTarget != null) return
        syncTarget = target
        syncStage = null
        syncProgress = if (target == DataSyncTarget.FULL) 0f else null
        syncProgressBase = 0f
        syncProgressWeight = 1f
        viewModelScope.launch {
            try {
                action()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message = error.message ?: "数据同步失败"
            } finally {
                syncStage = null
                syncTarget = null
                syncProgress = null
            }
        }
    }

    private fun syncBjmScoresAfterLogin() {
        startDataSync(DataSyncTarget.BJM_SCORES) {
            syncStage = "正在同步用户成绩"
            val count = syncBjmScoresData()
            syncStage = "正在构建索引"
            rebuildFullBjmIndex()
            message = "已同步 $count 条 BJM 成绩"
        }
    }

    internal fun syncAllData(automatic: Boolean = false) {
        startDataSync(DataSyncTarget.FULL) {
            val targets = if (automatic) automaticDataSyncTargets() else fullDataSyncTargets()
            if (targets.isEmpty()) return@startDataSync
            val totalSteps = targets.size + 1
            var completedSteps = 0
            fun beginStep(stage: String) {
                syncStage = stage
                syncProgressBase = completedSteps.toFloat() / totalSteps
                syncProgressWeight = 1f / totalSteps
                syncProgress = syncProgressBase
            }
            fun completeStep() {
                completedSteps += 1
                syncProgress = completedSteps.toFloat() / totalSteps
            }

            targets.forEach { target ->
                when (target) {
                    DataSyncTarget.TEXTAGE -> {
                        beginStep("正在同步 Textage 曲目库")
                        syncTextageData()
                    }
                    DataSyncTarget.BJM_MUSIC -> {
                        beginStep("正在同步 BJM 曲目库")
                        syncBjmMusicData()
                    }
                    DataSyncTarget.BJM_SCORES -> {
                        beginStep("正在同步用户成绩")
                        syncBjmScoresData()
                    }
                    DataSyncTarget.FULL -> Unit
                }
                completeStep()
            }
            syncStage = "正在构建索引"
            rebuildFullBjmIndex()
            completeStep()
            syncProgress = 1f
            if (!automatic) message = "全量数据同步完成"
        }
    }

    private fun fullDataSyncTargets(): List<DataSyncTarget> = buildList {
        add(DataSyncTarget.TEXTAGE)
        add(DataSyncTarget.BJM_MUSIC)
        if (appState.bjmUser != null) add(DataSyncTarget.BJM_SCORES)
    }

    private fun automaticDataSyncTargets(now: Long = System.currentTimeMillis()): List<DataSyncTarget> = buildList {
        if (!store.isTextageSyncComplete() || !isSameLocalDate(store.textageLastSyncAt(), now)) {
            add(DataSyncTarget.TEXTAGE)
        }
        if (appState.bjmMusic.isEmpty() || !isSameLocalDate(store.bjmMusicRevision(), now)) {
            add(DataSyncTarget.BJM_MUSIC)
        }
        if (appState.bjmUser != null && !isSameLocalDate(store.bjmScoresRevision(), now)) {
            add(DataSyncTarget.BJM_SCORES)
        }
    }

    internal fun syncTextageOnly() {
        startDataSync(DataSyncTarget.TEXTAGE) {
            syncStage = "正在同步 Textage 曲目库"
            syncTextageData()
            syncStage = "正在构建索引"
            rebuildTextageBjmIndex()
            message = "Textage 曲目库同步完成"
        }
    }

    internal fun syncBjmMusicOnly() {
        startDataSync(DataSyncTarget.BJM_MUSIC) {
            syncStage = "正在同步 BJM 曲目库"
            syncBjmMusicData()
            syncStage = "正在构建索引"
            rebuildTextageBjmIndex()
            message = "BJM 曲目库同步完成"
        }
    }

    internal fun syncBjmScoresOnly() {
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
        textageProgress = TextageSyncProgress(initial, 0, 0, "正在获取全部曲目元数据…")
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
                        val progress = TextageSyncProgress(initial, completed, total, title)
                        textageProgress = progress
                        if (syncTarget == DataSyncTarget.FULL) {
                            syncProgress = syncProgressBase + progress.fraction * syncProgressWeight
                        } else if (syncTarget == DataSyncTarget.TEXTAGE) {
                            syncProgress = progress.fraction
                        }
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
            val songGroups = withContext(Dispatchers.Default) {
                buildSongGroups(merged)
            }
            val nextState = appState.copy(charts = merged, songGroups = songGroups)
            appState = nextState
            val textageRevision = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                store.save(nextState)
                store.setTextageSyncComplete(true)
                store.setTextageLastSyncAt(textageRevision)
            }
            textageLastSyncAt = textageRevision
            textageProgress = TextageSyncProgress(
                initial = initial,
                completed = textageProgress?.total ?: imported.size,
                total = textageProgress?.total ?: imported.size,
                currentTitle = "曲目元数据获取完成",
            )
            textageProgress = null
        } catch (error: Exception) {
            if (error is CancellationException) throw error
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
        bjmMusicLastSyncAt = revision
    }

    private suspend fun syncBjmScoresData(): Int {
        val result = try {
            bjmClient.fetchScores()
        } catch (error: BjmException) {
            if (error.message?.contains("登录态不可用") == true) {
                clearBjmSession("BJM 登录已失效，请重新登录")
            }
            throw error
        }
        val mergedHistory = withContext(Dispatchers.IO) {
            val previousHistory = store.loadBjmHistory()
            appendBjmHistory(previousHistory, result.scores).also(store::saveBjmHistory)
        }
        val nextState = appState.copy(
            bjmScores = result.scores,
            bjmUser = result.user,
            bjmSyncedAt = System.currentTimeMillis(),
        )
        val revision = System.currentTimeMillis()
        appState = nextState
        bjmHistory = mergedHistory
        withContext(Dispatchers.IO) {
            store.setBjmScoresRevision(revision)
            store.save(nextState)
        }
        bjmScoresLastSyncAt = revision
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
            rebuildBjmCatalogIndex(
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

    internal fun openChart(chart: IidxChart) {
        selectedChart = chart
        selectedChartData = null
        chartLoading = true

        viewModelScope.launch {
            var fetchedPage: TextageChartPage? = null
            try {
                val cached = withContext(Dispatchers.IO) { store.loadChartData(chart) }
                val usableCached = cached?.takeIf { cachedData ->
                    cachedData.parsed && (chart.notes <= 0 || cachedData.chart.notes == chart.notes)
                }
                val data = usableCached ?: run {
                    if (chart.textageUrl == null) throw TextageException("该谱面没有可用的 Textage 链接")
                    fetchedPage = textageClient.fetchChartPage(chart)
                    val fetched = textageClient.parseChart(fetchedPage!!, chart)
                    withContext(Dispatchers.IO) { store.saveChartData(fetched) }
                    fetched
                }
                if (selectedChart?.id == chart.id) selectedChartData = data
                val siblings = chartFamily(chart)
                    .filter { it.id != chart.id && it.textageUrl != null }
                if (siblings.isNotEmpty()) {
                    val page = fetchedPage
                    viewModelScope.launch(Dispatchers.IO) {
                        warmChartFamily(chart, siblings, page)
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (selectedChart?.id == chart.id) {
                    selectedChartData = null
                    message = error.message ?: "谱面数据获取失败"
                }
            } finally {
                if (selectedChart?.id == chart.id) chartLoading = false
            }
        }
    }

    private fun chartFamily(chart: IidxChart): List<IidxChart> {
        val group = appState.songGroups.firstOrNull { it.key == songGroupKey(chart) }
        val chartIds = group?.chartIds?.toSet()
        return appState.charts.filter { candidate ->
            candidate.id in (chartIds ?: emptySet())
        }
    }

    private suspend fun warmChartFamily(
        selectedChart: IidxChart,
        siblings: List<IidxChart>,
        initialPage: TextageChartPage?,
    ) {
        val page = initialPage ?: runCatching {
            textageClient.fetchChartPage(selectedChart)
        }.getOrNull() ?: return
        siblings.forEach { sibling ->
            val cached = runCatching { store.loadChartData(sibling) }.getOrNull()
            val usableCached = cached?.takeIf { cachedData ->
                cachedData.parsed && (sibling.notes <= 0 || cachedData.chart.notes == sibling.notes)
            }
            if (usableCached == null) {
                runCatching {
                    textageClient.parseChart(page, sibling)
                }.onSuccess { parsed ->
                    runCatching { store.saveChartData(parsed) }
                }
            }
        }
    }

    internal fun openSong(chart: IidxChart, fromBjmHistory: Boolean? = null) {
        fromBjmHistory?.let { returnToBjmHistory = it }
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
        if (returnToBjmHistory) {
            returnToBjmHistory = false
            destination = AppDestination.HISTORY
        }
    }

    internal fun handleBack() {
        if (selectedChart != null) closeChart() else if (selectedSong != null) closeSong() else requestExit()
    }

    internal fun requestExit() {
        if (exitToastShown) {
            eventsChannel.trySend(AppEvent.Exit)
            return
        }
        exitToastShown = true
        message = "再返回一次以退出"
        viewModelScope.launch {
            delay(2_200L)
            exitToastShown = false
        }
    }

    internal fun savePlayerSettings(mode: String, settings: PlayerSettings) {
        val normalized = settings.copy(speed = settings.safeSpeed)
        if (mode == "DP") {
            dpPlayerSettings = normalized
        } else {
            spPlayerSettings = normalized
        }
        store.savePlayerSettings(mode, normalized)
    }

    internal fun clearChartCache() {
        viewModelScope.launch(Dispatchers.IO) {
            store.clearChartCache()
            withContext(Dispatchers.Main) {
                selectedChartData = null
                message = "已清除谱面缓存"
            }
        }
    }
}
