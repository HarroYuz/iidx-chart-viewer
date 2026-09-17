package com.harroyuz.iidxchartviewer.ui.player

import com.harroyuz.iidxchartviewer.ui.history.formatBjmHistoryTime
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import com.harroyuz.iidxchartviewer.ui.theme.Background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import com.harroyuz.iidxchartviewer.domain.catalog.songGroupKey
import com.harroyuz.iidxchartviewer.ui.motion.browseSharedBounds
import com.harroyuz.iidxchartviewer.ui.motion.browseSongSurface
import com.harroyuz.iidxchartviewer.ui.motion.browseReveal
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.model.TextageChartData
import com.harroyuz.iidxchartviewer.domain.player.PlayerSettings
import com.harroyuz.iidxchartviewer.domain.score.scoreRankName
import com.harroyuz.iidxchartviewer.ui.catalog.DifficultyChip
import com.harroyuz.iidxchartviewer.ui.components.AppTopBar
import com.harroyuz.iidxchartviewer.ui.components.ChartLoadError
import com.harroyuz.iidxchartviewer.ui.components.ChartParseWarning
import com.harroyuz.iidxchartviewer.ui.catalog.SongInfoHeader
import com.harroyuz.iidxchartviewer.ui.components.PlayStyleButton
import com.harroyuz.iidxchartviewer.ui.components.StrokedText
import com.harroyuz.iidxchartviewer.ui.components.clearFlagColor
import com.harroyuz.iidxchartviewer.ui.components.clearFlagDetailName
import com.harroyuz.iidxchartviewer.ui.components.difficultyColor
import com.harroyuz.iidxchartviewer.ui.components.difficultyName
import com.harroyuz.iidxchartviewer.ui.components.rankDeltaColor
import com.harroyuz.iidxchartviewer.ui.components.rankDeltaText
import com.harroyuz.iidxchartviewer.ui.components.hasUnmatchedScore
import com.harroyuz.iidxchartviewer.ui.components.scoreForChart
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.NormalBlue
import com.harroyuz.iidxchartviewer.ui.theme.Purple

@Composable
internal fun ChartDetailScreen(
    chart: IidxChart,
    siblingCharts: List<IidxChart>,
    bjmIndex: BjmIndex,
    chartData: TextageChartData?,
    loading: Boolean,
    playerSettings: PlayerSettings,
    visualEffectsDisabled: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    mode: String,
    onStyleToggle: () -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    onCopyText: (String) -> Unit,
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
) {
    var configExpanded by remember(chart.id) { mutableStateOf(false) }
    var headerHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    BackHandler(configExpanded) { configExpanded = false }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val collapsedPlayerHeight = (maxHeight - headerHeight).coerceAtLeast(0.dp)
        Box(Modifier.matchParentSize().browseSongSurface("song-surface:${songGroupKey(chart)}").background(Background))
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = !configExpanded,
                enter = if (visualEffectsDisabled) EnterTransition.None else expandVertically(tween(160), expandFrom = Alignment.Top) + fadeIn(tween(120)),
                exit = if (visualEffectsDisabled) ExitTransition.None else shrinkVertically(tween(160), shrinkTowards = Alignment.Top) + fadeOut(tween(100)),
            ) {
                Column(Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } }) {
                    AppTopBar(title = "谱面浏览", onNavigate = onBack) {
                        PlayStyleButton(mode, onStyleToggle)
                    }
                    SongInfoHeader(
                        title = chart.title,
                        sourceLabel = chart.sourceLabel,
                        subtitle = chart.subtitle,
                        genre = chart.genre,
                        composer = chart.composer,
                        version = chart.version,
                        status = chart.arcadeStatus,
                        bpm = chartData?.chart?.bpm?.ifBlank { chart.bpm } ?: chart.bpm,
                        songKey = songGroupKey(chart),
                        onCopyText = onCopyText,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        detail = true,
                        notes = (chartData?.chart?.notes ?: chart.notes).takeIf { it > 0 }?.toString() ?: "—",
                    )
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${chart.mode} ${difficultyName(chart.difficulty)} ${chart.level}${chart.score?.let { " · EX $it" } ?: ""}",
                                    color = difficultyColor(chart.difficulty),
                                    fontSize = 10.sp,
                                    letterSpacing = .8.sp,
                                )
                                Spacer(Modifier.height(7.dp))
                                Row(
                                    Modifier.fillMaxWidth().height(34.dp).horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    siblingCharts.forEach { sibling ->
                                        DifficultyChip(sibling, onOpenChart, selected = sibling.id == chart.id, sharedDifficulty = true)
                                    }
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            ChartScoreSummary(
                                score = scoreForChart(chart, bjmIndex, chartData?.chart?.notes ?: chart.notes),
                                showTime = true,
                                unmatchedScore = hasUnmatchedScore(chart, bjmIndex, chartData?.chart?.notes ?: chart.notes),
                                noteCount = chartData?.chart?.notes ?: chart.notes,
                                modifier = Modifier.browseSharedBounds("score:${chart.id}", stable = true),
                            )
                  }
            }
            Spacer(Modifier.height(16.dp))
                }
            }

            Box(Modifier.fillMaxWidth().weight(1f).browseReveal(fromBottom = true).background(Background)) {
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
                        configExpanded = configExpanded,
                        onConfigExpandedChange = { configExpanded = it },
                        collapsedPlayerHeight = collapsedPlayerHeight,
                        visualEffectsDisabled = visualEffectsDisabled,
                        onSettingsChange = onPlayerSettingsChange,
                        modifier = Modifier.fillMaxSize(),
                    )
                  }
            }
        }
    }
}

@Composable
internal fun ChartScoreSummary(
    score: BjmScore?,
    noteCount: Int,
    modifier: Modifier = Modifier,
    showTime: Boolean = false,
    unmatchedScore: Boolean = false,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (score == null) {
            Text(if (unmatchedScore) "成绩未匹配" else "NO PLAY", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(score.exScore.toString(), color = NormalBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text("(", color = Muted, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                StrokedText(
                    text = scoreRankName(score.exScore, noteCount),
                    fillColor = ComposeColor.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    strokeWidth = 1.3f,
                )
                rankDeltaText(score.exScore, noteCount)
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        Spacer(Modifier.width(3.dp))
                        Text(it, color = rankDeltaColor(it), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                Text(")", color = Muted, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
            if (showTime) {
                val date = remember(score.time) {
                    formatBjmHistoryTime(score.time)
                }
                Text(date, color = Muted, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1)
            }
        }
    }
}
