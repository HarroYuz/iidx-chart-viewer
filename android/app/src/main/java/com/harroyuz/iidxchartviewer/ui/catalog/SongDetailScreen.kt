package com.harroyuz.iidxchartviewer.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.score.scoreRankName
import com.harroyuz.iidxchartviewer.ui.components.AppTopBar
import com.harroyuz.iidxchartviewer.ui.components.AutoScrollingText
import com.harroyuz.iidxchartviewer.ui.components.DetailStat
import com.harroyuz.iidxchartviewer.ui.components.PlayStyleButton
import com.harroyuz.iidxchartviewer.ui.components.StrokedText
import com.harroyuz.iidxchartviewer.ui.components.clearFlagColor
import com.harroyuz.iidxchartviewer.ui.components.clearFlagDetailName
import com.harroyuz.iidxchartviewer.ui.components.difficultyColor
import com.harroyuz.iidxchartviewer.ui.components.difficultyName
import com.harroyuz.iidxchartviewer.ui.components.rankDeltaColor
import com.harroyuz.iidxchartviewer.ui.components.rankDeltaText
import com.harroyuz.iidxchartviewer.ui.components.scoreForChart
import com.harroyuz.iidxchartviewer.ui.theme.Background
import com.harroyuz.iidxchartviewer.ui.theme.CardSurface
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.NormalBlue

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
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "曲目信息", onNavigate = onBack) {
            PlayStyleButton(mode, onStyleToggle)
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
    val shape = MaterialTheme.shapes.medium
    val available = chart.textageUrl != null
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(if (available) CardSurface else Background)
            .border(1.dp, accent.copy(alpha = if (available) .35f else .15f), shape)
            .clickable(enabled = available) { onOpenChart(chart) }
            .heightIn(min = 72.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
                        Text(it, color = rankDeltaColor(it), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                Text(")", color = Muted, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
