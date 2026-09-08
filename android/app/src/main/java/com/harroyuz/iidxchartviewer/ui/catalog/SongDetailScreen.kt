package com.harroyuz.iidxchartviewer.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextOverflow
import com.harroyuz.iidxchartviewer.ui.history.formatBjmHistoryTime
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
import com.harroyuz.iidxchartviewer.ui.components.AutoScrollingText
import com.harroyuz.iidxchartviewer.ui.components.DetailStat
import com.harroyuz.iidxchartviewer.ui.components.PlayStyleButton
import com.harroyuz.iidxchartviewer.ui.components.difficultyColor
import com.harroyuz.iidxchartviewer.ui.components.difficultyName
import com.harroyuz.iidxchartviewer.ui.components.scoreForChart
import com.harroyuz.iidxchartviewer.ui.theme.Background
import com.harroyuz.iidxchartviewer.ui.theme.CardSurface
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted

@Composable
internal fun SongDetailScreen(
    song: IidxChart,
    charts: List<IidxChart>,
    bjmIndex: BjmIndex,
    mode: String,
    onBack: () -> Unit,
    onStyleToggle: () -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    onCopyText: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().browseSongSurface("song-surface:${songGroupKey(song)}").background(Background)) {
        AppTopBar(title = "曲目信息", onNavigate = onBack) {
            PlayStyleButton(mode, onStyleToggle)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f).browseSharedBounds("song-info:${songGroupKey(song)}", stableInDetails = true).padding(end = 12.dp)) {
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
                DetailStat("版本 ", song.version.ifBlank { "—" }, Modifier.browseSharedBounds("version:${songGroupKey(song)}", stableInDetails = true))
                DetailStat("BPM ", song.bpm.ifBlank { "—" }, Modifier.browseSharedBounds("bpm:${songGroupKey(song)}", stableInDetails = true))
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
    val scoreDate = remember(score?.time) { score?.let { formatBjmHistoryTime(it.time) } }
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
                if (scoreDate != null) {
                    Text(
                        scoreDate,
                        color = Muted,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ChartScoreSummary(
                score = score,
                noteCount = chart.notes,
                modifier = Modifier.browseSharedBounds("score:${chart.id}", stable = true),
            )
        }
    }
}
