package com.harroyuz.iidxchartviewer.ui.catalog

import com.harroyuz.iidxchartviewer.domain.model.BjmChartMetadata
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.harroyuz.iidxchartviewer.ui.components.DifficultyBackground
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.harroyuz.iidxchartviewer.domain.catalog.songGroupKey
import com.harroyuz.iidxchartviewer.ui.motion.browseSharedBounds
import com.harroyuz.iidxchartviewer.ui.motion.browseSongSurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.ui.player.ChartScoreSummary
import com.harroyuz.iidxchartviewer.ui.components.AppTopBar
import com.harroyuz.iidxchartviewer.ui.components.PlayStyleButton
import com.harroyuz.iidxchartviewer.ui.components.difficultyColor
import com.harroyuz.iidxchartviewer.ui.components.difficultyName
import com.harroyuz.iidxchartviewer.ui.components.hasUnmatchedScore
import com.harroyuz.iidxchartviewer.ui.components.scoreForChart
import com.harroyuz.iidxchartviewer.ui.theme.Background
import com.harroyuz.iidxchartviewer.ui.theme.Muted

@Composable
internal fun SongDetailScreen(
    song: IidxChart,
    initialRadarDifficulty: String?,
    chartMetadata: BjmChartMetadata?,
    chartMetadataLoading: Boolean,
    visualEffectsDisabled: Boolean,
    chartMetadataError: String?,
    onRefreshChartMetadata: () -> Unit,
    charts: List<IidxChart>,
    bjmIndex: BjmIndex,
    mode: String,
    onBack: () -> Unit,
    onStyleToggle: () -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    onCopyText: (String) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().browseSongSurface("song-surface:${songGroupKey(song)}").background(Background))
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = "曲目信息", onNavigate = onBack) {
                PlayStyleButton(mode, onStyleToggle)
            }
            SongInfoHeader(
                title = song.title,
                sourceLabel = song.sourceLabel,
                subtitle = song.subtitle,
                genre = song.genre,
                composer = song.composer,
                version = song.version,
                status = song.arcadeStatus,
                bpm = song.bpm,
                songKey = songGroupKey(song),
                onCopyText = onCopyText,
                modifier = Modifier.padding(horizontal = 20.dp),
                detail = true,
            )
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
                        unmatchedScore = hasUnmatchedScore(chart, bjmIndex),
                        onOpenChart = onOpenChart,
                    )
                }
                item(key = "radar") {
                    ChartRadarPanel(
                        songKey = songGroupKey(song),
                        initialDifficulty = initialRadarDifficulty,
                        charts = charts,
                        musicId = bjmIndex.songMusicIds[songGroupKey(song)],
                        mode = mode,
                        metadata = chartMetadata,
                        loading = chartMetadataLoading,
                        visualEffectsDisabled = visualEffectsDisabled,
                        error = chartMetadataError,
                        onRetry = onRefreshChartMetadata,
                    )
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }
}

@Composable
private fun DifficultyScoreCard(
    chart: IidxChart,
    score: BjmScore?,
    unmatchedScore: Boolean,
    onOpenChart: (IidxChart) -> Unit,
) {
    val accent = difficultyColor(chart.difficulty)
    val shape = MaterialTheme.shapes.medium
    val available = chart.textageUrl != null
    Box(
        Modifier.fillMaxWidth()
            .browseSharedBounds("difficulty:${chart.id}", overlayZ = 1f)
            .heightIn(min = 72.dp),
    ) {
        DifficultyBackground(chart, modifier = Modifier.matchParentSize(), detail = true)
        Row(
            Modifier.fillMaxWidth()
                .clip(shape)
                .clickable(enabled = available) { onOpenChart(chart) }
                .heightIn(min = 72.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${difficultyName(chart.difficulty)} ${chart.level}",
                    color = accent,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    "${chart.notes.takeIf { it > 0 } ?: "—"} NOTES",
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                ChartScoreSummary(
                    score = score,
                    noteCount = chart.notes,
                    showTime = true,
                    unmatchedScore = unmatchedScore,
                    modifier = Modifier.browseSharedBounds("score:${chart.id}", stable = true),
                )
            }
        }
    }
}
