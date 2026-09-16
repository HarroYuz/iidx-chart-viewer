package com.harroyuz.iidxchartviewer.ui.catalog

import com.harroyuz.iidxchartviewer.domain.catalog.preferredChartOrder
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.harroyuz.iidxchartviewer.domain.catalog.songGroupKey
import com.harroyuz.iidxchartviewer.ui.motion.browseSharedBounds
import androidx.compose.runtime.CompositionLocalProvider
import com.harroyuz.iidxchartviewer.ui.motion.LocalBrowseMotion
import com.harroyuz.iidxchartviewer.ui.components.DifficultyBackground
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.score.listScoreRankName
import com.harroyuz.iidxchartviewer.ui.components.clearFlagColor
import com.harroyuz.iidxchartviewer.ui.components.clearFlagShortName
import com.harroyuz.iidxchartviewer.ui.components.difficultyColor
import com.harroyuz.iidxchartviewer.ui.components.scoreForChart
import com.harroyuz.iidxchartviewer.ui.theme.CardSurface
import com.harroyuz.iidxchartviewer.ui.theme.Ink
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
    val motion = LocalBrowseMotion.current
    CompositionLocalProvider(
        LocalBrowseMotion provides motion?.takeIf { it.movingSongKey == null || it.movingSongKey == song.key },
    ) {
        SongGroupContent(song, bjmIndex, onOpenSong, onOpenChart, onCopyText, modifier)
    }
}

@Composable
private fun SongGroupContent(
    song: SongGroup,
    bjmIndex: BjmIndex,
    onOpenSong: (IidxChart) -> Unit,
    onOpenChart: (IidxChart) -> Unit,
    onCopyText: (String) -> Unit,
    modifier: Modifier,
) {
    val representative = song.charts.maxWithOrNull(preferredChartOrder)
    Column(
        modifier.fillMaxWidth()
            .browseSharedBounds(representative?.let { "song-surface:${songGroupKey(it)}" })
            .clip(MaterialTheme.shapes.medium)
            .background(CardSurface)
            .border(1.dp, Outline, MaterialTheme.shapes.medium)
            .clickable(enabled = representative != null) { representative?.let(onOpenSong) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        SongInfoHeader(
            title = song.title,
            sourceLabel = song.sourceLabel,
            subtitle = song.subtitle,
            genre = song.genre,
            composer = song.composer,
            version = song.version,
            status = representative?.arcadeStatus,
            bpm = song.charts.firstOrNull()?.bpm.orEmpty(),
            songKey = representative?.let(::songGroupKey),
            onCopyText = onCopyText,
        )
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            song.charts.forEach { chart ->
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
            .browseSharedBounds(if (sharedDifficulty) "difficulty:${chart.id}" else null, overlayZ = 1f),
    ) {
        DifficultyBackground(
            chart = chart,
            selected = selected,
            shared = sharedDifficulty,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier.fillMaxSize()
                .clip(shape)
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
