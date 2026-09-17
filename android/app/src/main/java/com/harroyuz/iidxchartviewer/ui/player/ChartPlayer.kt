package com.harroyuz.iidxchartviewer.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import com.harroyuz.iidxchartviewer.domain.player.*
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.State
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.harroyuz.iidxchartviewer.domain.model.TextageChartData
import com.harroyuz.iidxchartviewer.domain.player.PlayerSettings
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.NormalBlue
import com.harroyuz.iidxchartviewer.ui.theme.Orange
import com.harroyuz.iidxchartviewer.ui.theme.PlayerBackground
import com.harroyuz.iidxchartviewer.ui.theme.Purple
import java.util.Locale
import kotlinx.coroutines.isActive

private fun formatPlayerTime(seconds: Float): String {
    val total = seconds.coerceAtLeast(0f).toInt()
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

internal fun formatPlayerBpm(bpm: Float): String {
    val rounded = bpm.toInt()
    return if (kotlin.math.abs(bpm - rounded) < 0.01f) {
        rounded.toString()
    } else {
        String.format(Locale.US, "%.1f", bpm)
    }
}

@Composable
private fun PlayerLiveStats(
    data: TextageChartData,
    currentBeatState: State<Float>,
    totalMeasures: Int,
) {
    val currentBeat = currentBeatState.value
    val currentMeasure = data.measureAt(currentBeat).coerceIn(1, totalMeasures)
    val passedNotes = data.passedNoteCount(currentBeat)
    val totalNotes = data.chart.notes.takeIf { it > 0 } ?: data.totalNoteCount()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerTimeline(
    data: TextageChartData,
    currentBeatState: State<Float>,
    duration: Float,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onSeek: (Float) -> Unit,
) {
    val currentBeat = currentBeatState.value
    val totalSeconds = remember(data.chart.id, duration) {
        data.secondsAtBeat(duration).coerceAtLeast(0.001f)
    }
    val currentSeconds = data.secondsAtBeat(currentBeat)
    val progress = (currentSeconds / totalSeconds).coerceIn(0f, 1f)
    val currentBpm = data.bpmAt(currentBeat)

    Column(modifier) {
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
                onPlayingChange(false)
                onSeek(data.beatAtSeconds(it * totalSeconds).coerceIn(0f, duration))
            },
            thumb = {
                Box(Modifier.size(4.dp, 20.dp).background(Purple, RoundedCornerShape(2.dp)))
            },
            track = { sliderState ->
                // A continuous track avoids the default thumb gap hiding early progress.
                Canvas(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))) {
                    drawRect(Purple.copy(alpha = 0.18f))
                    drawRect(Purple, size = Size(size.width * sliderState.value.coerceIn(0f, 1f), size.height))
                }
            },
            modifier = Modifier.fillMaxWidth().height(24.dp),
        )
    }
}

@Composable
internal fun ChartPlayer(
    data: TextageChartData,
    settings: PlayerSettings,
    configExpanded: Boolean,
    onConfigExpandedChange: (Boolean) -> Unit,
    collapsedPlayerHeight: Dp,
    visualEffectsDisabled: Boolean,
    onSettingsChange: (PlayerSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var playing by remember(data.chart.id) { mutableStateOf(false) }
    val currentBeatState = remember(data.chart.id) { mutableStateOf(0f) }
    var beatBeforeConfig by remember(data.chart.id) { mutableStateOf<Float?>(null) }
    var previewSettings by remember(data.chart.id) { mutableStateOf<PlayerSettings?>(null) }
    val activeSettings = previewSettings ?: settings
    val safeSpeed = activeSettings.safeSpeed
    val safeSpeedMode = activeSettings.safeSpeedMode
    val safeGreenNumber = activeSettings.safeGreenNumber
    var rotaryKind by remember { mutableStateOf<PlayerNumber?>(null) }
    var rotaryCenter by remember { mutableStateOf(Offset.Zero) }
    var rotaryPointer by remember { mutableStateOf(Offset.Zero) }
    var rotary by remember { mutableStateOf<RotaryAdjustment?>(null) }
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var controlsHeight by remember { mutableStateOf(94.dp) }
    val density = LocalDensity.current
    val fieldBudget = (collapsedPlayerHeight - controlsHeight - 40.dp).coerceAtLeast(2.dp)
    val tailHeight = minOf(28.8.dp, fieldBudget / 2)
    val geometry = PlayerFieldGeometry(
        maximumNoteHeight = (fieldBudget - tailHeight).value,
        tailHeight = tailHeight.value,
    )
    val fieldHeight = (geometry.noteHeight(activeSettings.safeWhiteNumber) + tailHeight.value).dp
    fun finishRotary() {
        previewSettings?.let(onSettingsChange)
        previewSettings = null
        rotaryKind = null
        rotary = null
    }
    BackHandler(rotaryKind != null) { finishRotary() }
    LaunchedEffect(data.chart.id, configExpanded) {
        if (configExpanded) {
            beatBeforeConfig = currentBeatState.value
            currentBeatState.value = 0f
            playing = true
        } else {
            if (rotaryKind != null) finishRotary()
            beatBeforeConfig?.let { savedBeat ->
                playing = false
                currentBeatState.value = savedBeat
                beatBeforeConfig = null
            }
        }
    }
    val duration = data.durationBeats.coerceAtLeast(4f)
    val totalMeasures = data.measureCount().coerceAtLeast(1)

    LaunchedEffect(data.chart.id, playing) {
        if (!playing) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            // A restored position must not advance while effect cancellation is pending.
            if (!playing) break
            if (lastFrameNanos == 0L) {
                lastFrameNanos = frameNanos
                continue
            }
            val seconds = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, .25f)
            lastFrameNanos = frameNanos
            val nextBeat = data.beatAtSeconds(data.secondsAtBeat(currentBeatState.value) + seconds)
            currentBeatState.value = nextBeat
            if (nextBeat >= duration) {
                currentBeatState.value = duration
                playing = false
            }
        }
    }

    Box(modifier.fillMaxWidth().clipToBounds().onGloballyPositioned { rootPosition = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            Column(Modifier.onSizeChanged { controlsHeight = with(density) { it.height.toDp() } }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("谱面播放器", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        PlayerLiveStats(data, currentBeatState, totalMeasures)
                    }
                    TextButton(
                        onClick = {
                            playing = false
                            val currentBeat = currentBeatState.value
                            val currentMeasure = data.measureAt(currentBeat).coerceIn(1, totalMeasures)
                            val measureStart = data.measureStart(currentMeasure)
                            val targetMeasure = if (currentBeat <= measureStart + 0.001f) {
                                currentMeasure - 1
                            } else {
                                currentMeasure
                            }
                            currentBeatState.value = data.measureStart(targetMeasure.coerceAtLeast(1))
                        },
                        modifier = Modifier.size(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) { Text("|‹", color = Purple, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    TextButton(
                        onClick = {
                            if (!playing && currentBeatState.value >= duration) currentBeatState.value = 0f
                            playing = !playing
                        },
                        modifier = Modifier.size(42.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) { Text(if (playing) "Ⅱ" else "▶", color = Purple, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
                    TextButton(
                        onClick = {
                            playing = false
                            val currentMeasure = data.measureAt(currentBeatState.value).coerceIn(1, totalMeasures)
                            currentBeatState.value = if (currentMeasure >= totalMeasures) {
                                duration
                            } else {
                                data.measureStart(currentMeasure + 1).coerceAtMost(duration)
                            }
                        },
                        modifier = Modifier.size(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) { Text("›|", color = Purple, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
                PlayerTimeline(
                    data = data,
                    currentBeatState = currentBeatState,
                    duration = duration,
                    onPlayingChange = { playing = it },
                    onSeek = { currentBeatState.value = it },
                )
                Spacer(Modifier.height(8.dp))
            }

            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val panelHeight = if (configExpanded) (maxHeight - fieldHeight - 8.dp).coerceAtLeast(80.dp) else 40.dp
                Box(
                    Modifier.fillMaxWidth()
                        .height(fieldHeight)
                        .align(Alignment.TopCenter)
                        .background(PlayerBackground),
                ) {
                    ChartCanvas(
                        data = data,
                        currentBeatState = currentBeatState,
                        speed = safeSpeed,
                        speedMode = safeSpeedMode,
                        greenNumber = safeGreenNumber,
                        keepSpeedAcrossBpm = activeSettings.keepSpeedAcrossBpm,
                        showBarLines = activeSettings.showBarLines,
                        showBpmChanges = activeSettings.showBpmChanges,
                        showMeasureNumbers = activeSettings.showMeasureNumbers,
                        side = activeSettings.side,
                        flip = activeSettings.flip,
                        playOption = activeSettings.safePlayOption,
                        playOption1P = activeSettings.safePlayOption1P,
                        playOption2P = activeSettings.safePlayOption2P,
                        randomMapping1P = activeSettings.safeRandomMapping1P,
                        randomMapping2P = activeSettings.safeRandomMapping2P,
                        tailHeightPx = with(density) { tailHeight.toPx() },
                        onScrubStart = { playing = false },
                        onCurrentBeatChange = { currentBeatState.value = it.coerceIn(0f, duration) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                PlayerConfigBox(
                    settings = activeSettings,
                    isSp = data.chart.mode != "DP",
                    expanded = configExpanded,
                    onExpandedChange = onConfigExpandedChange,
                    visualEffectsDisabled = visualEffectsDisabled,
                    maximumWhiteNumber = geometry.maximumWhiteNumber,
                    maximumPanelHeight = panelHeight,
                    onRotaryStart = { kind, center ->
                        rotaryKind = kind
                        rotaryCenter = center
                        rotaryPointer = center
                        previewSettings = activeSettings
                        val isHeight = kind == PlayerNumber.HEIGHT
                        val isFloating = activeSettings.safeSpeedMode == PLAYER_SPEED_MODE_FLOATING
                        val initial = if (isHeight) (1000 - activeSettings.safeWhiteNumber.coerceAtMost(geometry.maximumWhiteNumber)).toFloat()
                            else if (isFloating) activeSettings.safeGreenNumber.toFloat() else activeSettings.safeSpeed
                        rotary = RotaryAdjustment(initial,
                            if (isHeight) (1000 - geometry.maximumWhiteNumber).toFloat() else if (isFloating) PLAYER_GREEN_NUMBER_MIN.toFloat() else 1f,
                            if (isHeight) 1000f else if (isFloating) PLAYER_GREEN_NUMBER_MAX.toFloat() else 100f)
                    },
                    onRotaryMove = { point ->
                        rotaryPointer = point
                        val relative = point - rotaryCenter
                        val isHeight = rotaryKind == PlayerNumber.HEIGHT
                        val isFloating = activeSettings.safeSpeedMode == PLAYER_SPEED_MODE_FLOATING
                        val value = rotary?.move(relative.x, relative.y, with(density) { 12.dp.toPx() }, if (isHeight || isFloating) 200f else 1f)
                        if (value != null) previewSettings = when {
                            isHeight -> activeSettings.copy(whiteNumber = 1000 - value.roundToInt())
                            isFloating -> activeSettings.copy(greenNumber = value.roundToInt())
                            else -> activeSettings.copy(speed = normalizeHiSpeed(value))
                        }
                    },
                    onRotaryEnd = { finishRotary() },
                    onSettingsChange = { next ->
                        onSettingsChange(
                            next.copy(
                                speed = next.safeSpeed,
                                speedMode = next.safeSpeedMode,
                                greenNumber = next.safeGreenNumber,
                                keepSpeedAcrossBpm = next.keepSpeedAcrossBpm,
                            ),
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).zIndex(3f).alpha(if (rotaryKind == null) 1f else 0f),
                )
            }
            if (!data.parsed) Text(data.parserMessage ?: "当前谱面格式尚未完成解析。", color = Orange, fontSize = 10.sp)
        }
        if (rotaryKind != null) {
            val label = when {
                rotaryKind == PlayerNumber.HEIGHT -> "谱面区域高度 ${1000 - activeSettings.safeWhiteNumber.coerceAtMost(geometry.maximumWhiteNumber)}"
                activeSettings.safeSpeedMode == PLAYER_SPEED_MODE_FLOATING -> "FHS ${activeSettings.safeGreenNumber}"
                else -> "Hi-Speed ${String.format(Locale.US, "%.2f", activeSettings.safeSpeed)}"
            }
            PlayerRotaryOverlay(rotaryCenter - rootPosition, rotaryPointer - rootPosition, label)
        }
    }
}
