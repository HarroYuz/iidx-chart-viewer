package com.harroyuz.iidxchartviewer.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.harroyuz.iidxchartviewer.domain.catalog.songGroupKey
import com.harroyuz.iidxchartviewer.ui.motion.browseSharedBounds
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.score.listScoreRankName
import com.harroyuz.iidxchartviewer.ui.components.CopyableText
import com.harroyuz.iidxchartviewer.ui.components.clearFlagColor
import com.harroyuz.iidxchartviewer.ui.components.clearFlagShortName
import com.harroyuz.iidxchartviewer.ui.components.difficultyColor
import com.harroyuz.iidxchartviewer.ui.components.scoreForChart
import com.harroyuz.iidxchartviewer.ui.theme.Background
import com.harroyuz.iidxchartviewer.ui.theme.CardSurface
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.NormalBlue
import com.harroyuz.iidxchartviewer.ui.theme.Outline

@Composable
internal fun SongGroupRow(
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
            .browseSharedBounds(representative?.let { "song-surface:${songGroupKey(it)}" })
            .clip(MaterialTheme.shapes.medium)
            .background(CardSurface)
            .border(1.dp, Outline, MaterialTheme.shapes.medium)
            .clickable(enabled = representative != null) { representative?.let(onOpenSong) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).browseSharedBounds(representative?.let { "song-info:${songGroupKey(it)}" }).padding(end = 10.dp)) {
                CopyableText(
                    text = song.genre.ifBlank { "未知曲风" },
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onCopy = onCopyText,
                )
                Spacer(Modifier.height(4.dp))
                Text(displayTitle(song.title, song.sourceLabel), color = Ink, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (song.subtitle.isNotBlank()) {
                    Text(song.subtitle, color = Muted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    song.composer.ifBlank { "未知曲师" },
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(song.version.ifBlank { "—" }, modifier = Modifier.browseSharedBounds(representative?.let { "version:${songGroupKey(it)}" }), color = NormalBlue, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Muted)) { append("BPM ") }
                        withStyle(SpanStyle(color = NormalBlue)) {
                            append(song.charts.firstOrNull()?.bpm?.ifBlank { "—" } ?: "—")
                        }
                    },
                    modifier = Modifier.browseSharedBounds(representative?.let { "bpm:${songGroupKey(it)}" }),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        LazyRow(
            Modifier.fillMaxWidth().heightIn(min = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(song.charts, key = { it.id }) { chart ->
                DifficultyChip(
                    chart = chart,
                    onOpenChart = onOpenChart,
                    sharedDifficulty = true,
                    score = scoreForChart(chart, bjmIndex),
                )
            }
        }
    }
}

@Composable
internal fun DifficultyChip(
    chart: IidxChart,
    onOpenChart: (IidxChart) -> Unit,
    selected: Boolean = false,
    sharedDifficulty: Boolean = false,
    score: BjmScore? = null,
) {
    val accent = difficultyColor(chart.difficulty)
    val shape = RoundedCornerShape(14.dp)
    val available = chart.textageUrl != null
    Box(
        modifier = Modifier.size(width = 42.dp, height = 34.dp)
            .browseSharedBounds(if (sharedDifficulty) "difficulty:${chart.id}" else null),
    ) {
        Box(
            Modifier.fillMaxSize()
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
        if (score != null) {
            Text(
                clearFlagShortName(score.clearFlag),
                style = TextStyle(
                    color = clearFlagColor(score.clearFlag),
                    fontSize = 12.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart),
            )
            Text(
                listScoreRankName(score.exScore, chart.notes),
                style = TextStyle(
                    color = Ink,
                    fontSize = 12.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}
