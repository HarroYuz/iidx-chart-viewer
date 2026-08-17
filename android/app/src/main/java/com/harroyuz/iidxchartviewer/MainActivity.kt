package com.harroyuz.iidxchartviewer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
private const val TEXTAGE_SYNC_INTERVAL_MS = 24L * 60L * 60L * 1000L

class MainActivity : ComponentActivity() {
    private lateinit var store: IidxLocalStore
    private lateinit var bjmClient: BjmClient
    private lateinit var textageClient: TextageClient

    private var appState by mutableStateOf(IidxAppState())
    private var localCatalogPresent by mutableStateOf(false)
    private var bjmSyncing by mutableStateOf(false)
    private var textageSyncing by mutableStateOf(false)
    private var textageProgress by mutableStateOf<TextageSyncProgress?>(null)
    private var textageError by mutableStateOf<String?>(null)
    private var chartLoading by mutableStateOf(false)
    private var selectedChart by mutableStateOf<IidxChart?>(null)
    private var selectedChartData by mutableStateOf<TextageChartData?>(null)
    private var playerSettings by mutableStateOf(PlayerSettings())
    private var message by mutableStateOf<String?>(null)
    private var loginPending = false

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
                    bjmSyncing = bjmSyncing,
                    textageSyncing = textageSyncing,
                    textageProgress = textageProgress,
                    textageError = textageError,
                    selectedChart = selectedChart,
                    chartData = selectedChartData,
                    chartLoading = chartLoading,
                    playerSettings = playerSettings,
                    message = message,
                    localCatalogPresent = localCatalogPresent,
                    onDismissMessage = { message = null },
                    onLogin = ::openBjmLogin,
                    onOpenBjmProfile = {},
                    onSyncBjm = ::syncBjm,
                    onRefreshTextage = ::refreshTextage,
                    onOpenChart = ::openChart,
                    onBack = ::closeChart,
                    onRetryChart = { selectedChart?.let(::openChart) },
                    onPlayerSettingsChange = ::savePlayerSettings,
                )
            }
        }
        // Loading a large local catalog is also kept off the main thread.
        lifecycleScope.launch(Dispatchers.IO) {
            val loaded = store.load()
            withContext(Dispatchers.Main) {
                appState = loaded
                // Bootstrap only when there is no usable local catalog. A
                // completed catalog should open immediately; the update
                // button performs the next metadata refresh on demand.
                val needsBootstrap = loaded.charts.isEmpty() || !store.isTextageSyncComplete()
                val dailySyncDue = System.currentTimeMillis() - store.textageLastSyncAt() >= TEXTAGE_SYNC_INTERVAL_MS
                if (needsBootstrap || dailySyncDue) {
                    refreshTextage()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Covers devices where the WebView activity is recreated before the
        // ActivityResult callback reaches this activity.
        if (loginPending && !bjmSyncing) {
            loginPending = false
            syncBjm()
        }
    }

    private fun openBjmLogin() {
        loginPending = true
        loginLauncher.launch(Intent(this, BjmLoginActivity::class.java))
    }

    private fun syncBjm() {
        if (bjmSyncing) return
        bjmSyncing = true
        lifecycleScope.launch {
            try {
                val result = bjmClient.fetchScores()
                appState = appState.copy(
                    bjmScores = result.scores,
                    bjmUser = result.user,
                    bjmSyncedAt = System.currentTimeMillis(),
                )
                withContext(Dispatchers.IO) { store.save(appState) }
                message = "已同步 ${result.scores.size} 条 BJM 成绩"
            } catch (error: Exception) {
                message = error.message ?: "BJM 同步失败"
            } finally {
                bjmSyncing = false
            }
        }
    }

    private fun refreshTextage() {
        if (textageSyncing) return
        val initial = appState.charts.isEmpty() || !store.isTextageSyncComplete()
        textageSyncing = true
        textageError = null
        textageProgress = TextageSyncProgress(initial, 0, 0, "正在获取全部歌曲元数据…")
        lifecycleScope.launch {
            try {
                val imported = textageClient.fetchCatalog { completed, total, title ->
                    withContext(Dispatchers.Main) {
                        textageProgress = TextageSyncProgress(initial, completed, total, title)
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
                appState = appState.copy(charts = merged)
                textageProgress = TextageSyncProgress(
                    initial = initial,
                    completed = textageProgress?.total ?: imported.size,
                    total = textageProgress?.total ?: imported.size,
                    currentTitle = "歌曲元数据获取完成",
                )
                withContext(Dispatchers.IO) { store.save(appState) }
                store.setTextageSyncComplete(true)
                store.setTextageLastSyncAt()
                textageProgress = null
            } catch (error: Exception) {
                textageError = error.message ?: "Textage 更新失败"
                message = textageError
                if (!initial) textageProgress = null
            } finally {
                textageSyncing = false
            }
        }
    }

    private fun openChart(chart: IidxChart) {
        selectedChart = chart
        selectedChartData = null
        chartLoading = true

        lifecycleScope.launch {
            try {
                val cached = withContext(Dispatchers.IO) { store.loadChartData(chart) }
                val usableCached = cached?.takeIf { cachedData ->
                    chart.notes <= 0 || cachedData.notes.size == chart.notes
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

    private fun closeChart() {
        selectedChart = null
        selectedChartData = null
        chartLoading = false
    }

    private fun savePlayerSettings(settings: PlayerSettings) {
        playerSettings = settings.copy(speed = settings.safeSpeed)
        store.savePlayerSettings(playerSettings)
    }
}

@Composable
private fun IidxApp(
    state: IidxAppState,
    localCatalogPresent: Boolean,
    bjmSyncing: Boolean,
    textageSyncing: Boolean,
    textageProgress: TextageSyncProgress?,
    textageError: String?,
    selectedChart: IidxChart?,
    chartData: TextageChartData?,
    chartLoading: Boolean,
    playerSettings: PlayerSettings,
    message: String?,
    onDismissMessage: () -> Unit,
    onLogin: () -> Unit,
    onOpenBjmProfile: () -> Unit,
    onSyncBjm: () -> Unit,
    onRefreshTextage: () -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    onBack: () -> Unit,
    onRetryChart: () -> Unit,
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
) {
    Scaffold(containerColor = Background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            var browserMode by rememberSaveable { mutableStateOf("SP") }
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
                    mode = browserMode,
                    onModeChange = { browserMode = it },
                    bjmSyncing = bjmSyncing,
                    textageSyncing = textageSyncing,
                    textageProgress = textageProgress,
                    onLogin = onLogin,
                    onOpenBjmProfile = onOpenBjmProfile,
                    onSyncBjm = onSyncBjm,
                    onRefreshTextage = onRefreshTextage,
                    onOpenChart = onOpenChart,
                    modifier = Modifier.alpha(if (selectedChart == null) 1f else 0f),
                )
                if (selectedChart != null) {
                    val family = state.charts
                        .filter {
                            it.mode == selectedChart.mode &&
                                it.title == selectedChart.title &&
                                it.subtitle == selectedChart.subtitle &&
                                it.composer == selectedChart.composer
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
                            val alternate = state.charts
                                .filter {
                                    it.mode == targetMode &&
                                        it.title == selectedChart.title &&
                                        it.subtitle == selectedChart.subtitle &&
                                        it.composer == selectedChart.composer &&
                                        it.textageUrl != null
                                }
                                .maxWithOrNull(
                                    compareBy<IidxChart>({ difficultyOrder(it.difficulty) }, { it.level }, { it.notes }),
                                )
                            browserMode = targetMode
                            if (alternate != null) onOpenChart(alternate) else onBack()
                        },
                        onOpenChart = onOpenChart,
                        onPlayerSettingsChange = onPlayerSettingsChange,
                    )
                }
            }
            if (message != null) ToastCard(message, onDismissMessage, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun ChartBrowserScreen(
    state: IidxAppState,
    mode: String,
    onModeChange: (String) -> Unit,
    bjmSyncing: Boolean,
    textageSyncing: Boolean,
    textageProgress: TextageSyncProgress?,
    onLogin: () -> Unit,
    onOpenBjmProfile: () -> Unit,
    onSyncBjm: () -> Unit,
    onRefreshTextage: () -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredCharts = state.charts.filter {
        it.mode == mode && (query.isBlank() || "${it.title} ${it.subtitle} ${it.genre} ${it.composer}".contains(query, ignoreCase = true))
    }
    val songs = remember(filteredCharts) {
        filteredCharts.groupBy { chart ->
            listOf(chart.title, chart.subtitle, chart.composer).joinToString("\u0000")
        }.values.map { group ->
            SongGroup(
                key = group.first().id.substringBeforeLast('-'),
                title = group.first().title,
                subtitle = group.first().subtitle,
                genre = group.first().genre,
                composer = group.first().composer,
                version = group.first().version,
                charts = group.groupBy { it.difficulty }.values.map { sameDifficulty ->
                    sameDifficulty.maxWithOrNull(
                        compareBy<IidxChart>({ it.textageUrl != null }, { it.notes }, { it.bpm.isNotBlank() }),
                    ) ?: sameDifficulty.first()
                }.sortedWith(compareBy<IidxChart> { difficultyOrder(it.difficulty) }.thenBy { it.level }),
            )
        }
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                TextButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(42.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("☰", color = Ink, fontSize = 24.sp) }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("更新谱面数据") },
                        onClick = {
                            menuExpanded = false
                            onRefreshTextage()
                        },
                        enabled = !textageSyncing,
                    )
                    DropdownMenuItem(
                        text = { Text(if (bjmSyncing) "同步 BJM 成绩中…" else "同步 BJM 成绩") },
                        onClick = {
                            menuExpanded = false
                            onSyncBjm()
                        },
                        enabled = !bjmSyncing && state.bjmUser != null,
                    )
                }
            }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text("谱面浏览", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            placeholder = { Text("搜索曲名或艺术家", color = Muted) },
            singleLine = true,
        )
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(songs, key = { it.key }) { song ->
                SongGroupRow(song, onOpenChart, Modifier.padding(horizontal = 18.dp, vertical = 5.dp))
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

private data class SongGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val genre: String,
    val composer: String,
    val version: String,
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
        Text(if (error == null) "正在准备谱面库" else "谱面库获取失败", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            if (error == null) "首次启动会从 Textage 获取全部谱面并保存到本机。" else error,
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
private fun SongGroupRow(song: SongGroup, onOpenChart: (IidxChart) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth()
            .border(1.dp, ComposeColor(0xFF292B42), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(song.genre.ifBlank { "未知曲风" }, color = Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(song.title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (song.subtitle.isNotBlank()) {
                    Text(song.subtitle, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                Text(song.version.ifBlank { "—" }, color = Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "BPM ${song.charts.firstOrNull()?.bpm?.ifBlank { "—" } ?: "—"}",
                    color = Muted,
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
                DifficultyChip(chart, onOpenChart)
            }
        }
    }
}

@Composable
private fun DifficultyChip(
    chart: IidxChart,
    onOpenChart: (IidxChart) -> Unit,
    selected: Boolean = false,
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
        Text(if (chart.level > 0) chart.level.toString() else "—", color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回", color = Purple) }
            Text("谱面浏览", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                Text(chart.genre.ifBlank { "未知曲风" }, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(chart.title, color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (chart.subtitle.isNotBlank()) {
                    Text(chart.subtitle, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text(chart.composer.ifBlank { "未知曲师" }, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(chart.version.ifBlank { "—" }, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "BPM ${chartData?.chart?.bpm?.ifBlank { chart.bpm } ?: chart.bpm.ifBlank { "—" }}",
                    color = Muted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "NOTES ${(chartData?.chart?.notes ?: chart.notes).takeIf { it > 0 } ?: "—"}",
                    color = Muted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
) {
    val shape = RoundedCornerShape(10.dp)
    val summary = buildString {
        append("Hi-Speed: ${settings.safeSpeed}x")
        if (isSp) {
            append(", ${settings.side}")
            if (settings.mirror) append(", MIRROR")
        }
    }
    Column(
        Modifier.fillMaxWidth()
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
            Text(if (expanded) "▼" else "▲", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Box(
                    Modifier.width(52.dp).height(30.dp).border(1.dp, ComposeColor(0xFFCAC7D6), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("${settings.safeSpeed}x", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                TextButton(
                    onClick = { onSettingsChange(settings.copy(speed = (settings.safeSpeed + 1).coerceAtMost(50))) },
                    modifier = Modifier.size(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("+", color = Purple, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("小节线", color = Muted, fontSize = 11.sp)
                Switch(
                    checked = settings.showBarLines,
                    onCheckedChange = { onSettingsChange(settings.copy(showBarLines = it)) },
                    modifier = Modifier.padding(start = 3.dp),
                )
                if (isSp) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onSettingsChange(settings.copy(side = "1P")) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Text("1P", color = if (settings.side == "1P") Purple else Muted, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { onSettingsChange(settings.copy(side = "2P")) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Text("2P", color = if (settings.side == "2P") Purple else Muted, fontWeight = FontWeight.Bold)
                    }
                    Text("MIRROR", color = Muted, fontSize = 10.sp)
                    Switch(
                        checked = settings.mirror,
                        onCheckedChange = { onSettingsChange(settings.copy(mirror = it)) },
                    )
                }
            }
        }
    }
}

private fun formatPlayerTime(seconds: Float): String {
    val total = seconds.coerceAtLeast(0f).toInt()
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
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
    val currentMeasure = (kotlin.math.floor(currentBeat / 4f).toInt() + 1).coerceIn(1, kotlin.math.ceil(duration / 4f).toInt())
    val passedNotes = data.notes.count { it.beat <= currentBeat + 0.001f }
    val progress = (currentBeat / duration).coerceIn(0f, 1f)
    val currentSeconds = currentBeat * 60f / data.bpm.coerceAtLeast(1f)
    val totalSeconds = duration * 60f / data.bpm.coerceAtLeast(1f)

    LaunchedEffect(data.chart.id, playing) {
        if (!playing) return@LaunchedEffect
        var last = System.nanoTime()
        while (isActive) {
            delay(16L)
            val now = System.nanoTime()
            val seconds = (now - last) / 1_000_000_000f
            last = now
            currentBeat += seconds * data.bpm / 60f
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
                        withStyle(SpanStyle(color = Muted)) { append("NOTES ") }
                        withStyle(SpanStyle(color = NormalBlue)) { append("$passedNotes/${data.notes.size}") }
                        withStyle(SpanStyle(color = Muted)) { append(" · BPM ") }
                        withStyle(SpanStyle(color = NormalBlue)) { append(data.bpm.toInt().toString()) }
                        withStyle(SpanStyle(color = Muted)) { append(" · Measure ") }
                        withStyle(SpanStyle(color = NormalBlue)) { append("$currentMeasure/${kotlin.math.ceil(duration / 4f).toInt()}") }
                    },
                    fontSize = 10.sp,
                )
            }
            TextButton(
                onClick = {
                    playing = false
                    currentBeat = (kotlin.math.floor(currentBeat / 4f).toInt() - 1).coerceAtLeast(0) * 4f
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
                    currentBeat = ((kotlin.math.floor(currentBeat / 4f).toInt() + 1) * 4f).coerceAtMost(duration)
                },
                modifier = Modifier.size(34.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Text("›|", color = Purple, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatPlayerTime(currentSeconds), color = Muted, fontSize = 10.sp)
            Text(formatPlayerTime(totalSeconds), color = Muted, fontSize = 10.sp)
        }
        Slider(
            value = progress,
            onValueChange = {
                playing = false
                currentBeat = it * duration
            },
            modifier = Modifier.fillMaxWidth().height(24.dp),
        )

        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(PlayerBackground),
        ) {
            ChartCanvas(
                data = data,
                currentBeat = currentBeat,
                speed = safeSpeed,
                showBarLines = settings.showBarLines,
                side = settings.side,
                mirror = settings.mirror,
                playing = playing,
                onCurrentBeatChange = { currentBeat = it.coerceIn(0f, duration) },
                modifier = Modifier.fillMaxSize(),
            )
            if (data.chart.mode == "DP") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("1P", color = ComposeColor.White.copy(alpha = .55f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("2P", color = ComposeColor.White.copy(alpha = .55f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        PlayerConfigBox(
            settings = settings,
            isSp = data.chart.mode != "DP",
            expanded = configExpanded,
            onExpandedChange = { configExpanded = it },
            onSettingsChange = { next -> onSettingsChange(next.copy(speed = next.safeSpeed)) },
        )
        if (!data.parsed) Text(data.parserMessage ?: "当前谱面格式尚未完成解析。", color = Orange, fontSize = 10.sp)
    }
}

@Composable
private fun ChartCanvas(
    data: TextageChartData,
    currentBeat: Float,
    speed: Int,
    showBarLines: Boolean,
    side: String,
    mirror: Boolean,
    playing: Boolean,
    onCurrentBeatChange: (Float) -> Unit,
    modifier: Modifier,
) {
    val laneCount = if (data.chart.mode == "DP") 16 else 8
    // IIDX's scroll velocity is proportional to BPM x Hi-Speed. Playback
    // already advances beats using BPM, so the lane distance only needs to
    // scale linearly with the user's Hi-Speed value.
    val pixelsPerBeat = 16f * speed
    Canvas(
        modifier.pointerInput(data.chart.id, playing, speed) {
            detectVerticalDragGestures { _, dragAmount ->
                if (!playing) onCurrentBeatChange(currentBeat - dragAmount / pixelsPerBeat)
            }
        },
    ) {
        val isSp = data.chart.mode != "DP"
        val dpGapUnits = 1.5f
        val laneWidths = if (isSp) {
            if (side == "2P") List(7) { 1f } + listOf(1.5f) else listOf(1.5f) + List(7) { 1f }
        } else listOf(1.5f) + List(7) { 1f } + List(7) { 1f } + listOf(1.5f)
        val unit = if (isSp) size.width / laneWidths.sum() else size.width / (laneWidths.sum() + dpGapUnits)
        val laneLefts = laneWidths.runningFold(0f) { sum, width -> sum + width * unit }.dropLast(1)
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
        val judgeY = size.height * .84f

        drawRect(PlayerBackground)
        if (!isSp) {
            drawRect(
                color = PlayerCenterGap,
                topLeft = Offset(dpLeftWidth * unit, 0f),
                size = Size(dpGapUnits * unit, size.height),
            )
        }
        for (lane in 0..laneCount) {
            val x = if (isSp) laneLefts.getOrNull(lane) ?: size.width else dpBoundaryX(lane)
            drawLine(
                color = if (lane == 0 || lane == laneCount) PlayerEdge else PlayerLane,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = if (lane == 0 || lane == laneCount) 2f else 1f,
            )
        }
        if (!isSp) {
            drawLine(
                color = PlayerEdge,
                start = Offset((dpLeftWidth + dpGapUnits) * unit, 0f),
                end = Offset((dpLeftWidth + dpGapUnits) * unit, size.height),
                strokeWidth = 2f,
            )
        }
        if (showBarLines) {
            val firstBar = kotlin.math.floor((currentBeat - size.height / pixelsPerBeat) / 4f).toInt().coerceAtLeast(0)
            val lastBar = kotlin.math.ceil((currentBeat + size.height / pixelsPerBeat) / 4f).toInt()
            for (bar in firstBar..lastBar) {
                val y = judgeY - (bar * 4f - currentBeat) * pixelsPerBeat
                if (y in -2f..size.height + 2f) {
                    drawLine(ComposeColor(0xFF444756), Offset(0f, y), Offset(size.width, y), strokeWidth = if (bar % 4 == 0) 2f else 1f)
                }
            }
        }
        drawLine(PlayerRed, Offset(0f, judgeY), Offset(size.width, judgeY), strokeWidth = 5f)
        data.notes.forEach { note ->
            if (note.beat < currentBeat - 0.001f) return@forEach
            val y = judgeY - (note.beat - currentBeat) * pixelsPerBeat
            if (y !in -70f..size.height + 70f) return@forEach
            val rawLane = if (isSp) note.lane.mod(8) else note.lane.coerceIn(0, laneCount - 1)
            val logicalLane = if (isSp && mirror && rawLane > 0) 8 - rawLane else rawLane
            val displayLane = when {
                isSp && side == "2P" -> if (logicalLane == 0) 7 else logicalLane - 1
                !isSp && rawLane >= 8 -> 23 - rawLane
                else -> logicalLane
            }
            val laneIndex = displayLane.coerceIn(0, laneCount - 1)
            val laneWidth = laneWidths.getOrElse(laneIndex) { 1f } * unit
            val laneStart = if (isSp) {
                laneLefts.getOrElse(laneIndex) { 0f }
            } else {
                dpLaneStart(laneIndex)
            }
            val left = laneStart + laneWidth * .12f
            val width = laneWidth * .76f
            val sideLane = if (isSp) rawLane else if (rawLane >= 8) rawLane - 8 else rawLane
            val noteColor = if (!isSp) when (sideLane) {
                0 -> PlayerRed
                1, 3, 5, 7 -> ComposeColor.White
                else -> PlayerSkyBlue
            } else when {
                rawLane == 0 -> PlayerRed
                rawLane % 2 == 1 -> ComposeColor.White
                else -> PlayerSkyBlue
            }
            if (note.holdBeats > 0f) {
                val holdHeight = note.holdBeats * pixelsPerBeat
                val holdWidth = width * .88f
                val endY = y - holdHeight.coerceAtLeast(8f)
                drawRect(
                    color = noteColor.copy(alpha = .58f),
                    topLeft = Offset(left + (width - holdWidth) / 2f, endY),
                    size = Size(holdWidth, holdHeight.coerceAtLeast(8f)),
                )
                drawRoundRect(
                    color = noteColor,
                    topLeft = Offset(left, endY - 6f),
                    size = Size(width, 12f),
                    cornerRadius = CornerRadius(5f),
                )
            }
            drawRoundRect(
                color = noteColor,
                topLeft = Offset(left, y - 6f),
                size = Size(width, 12f),
                cornerRadius = CornerRadius(5f),
            )
        }
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

private fun difficultyName(value: String): String = when (value) {
    "B" -> "BEGINNER"
    "N" -> "NORMAL"
    "H" -> "HYPER"
    "A" -> "ANOTHER"
    "L" -> "LEGGENDARIA"
    else -> value
}

@Composable
private fun ToastCard(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.padding(14.dp).clickable { onDismiss() }, color = ComposeColor(0xFF1A2C27), shape = RoundedCornerShape(13.dp)) {
        Text(message, color = Green, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
}
