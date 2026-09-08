package com.harroyuz.iidxchartviewer.ui.player

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
internal fun ChartPlayer(
    data: TextageChartData,
    settings: PlayerSettings,
    onSettingsChange: (PlayerSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var playing by remember(data.chart.id) { mutableStateOf(false) }
    var currentBeat by remember(data.chart.id) { mutableStateOf(0f) }
    var configExpanded by remember(data.chart.id) { mutableStateOf(false) }
    val safeSpeed = settings.safeSpeed
    val safeSpeedMode = settings.safeSpeedMode
    val safeGreenNumber = settings.safeGreenNumber
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
                    speedMode = safeSpeedMode,
                    greenNumber = safeGreenNumber,
                    keepSpeedAcrossBpm = settings.keepSpeedAcrossBpm,
                    showBarLines = settings.showBarLines,
                    showBpmChanges = settings.showBpmChanges,
                    showMeasureNumbers = settings.showMeasureNumbers,
                    side = settings.side,
                    flip = settings.flip,
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
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(3f),
            )
        }
        if (!data.parsed) Text(data.parserMessage ?: "当前谱面格式尚未完成解析。", color = Orange, fontSize = 10.sp)
    }
}
