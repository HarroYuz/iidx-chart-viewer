package com.harroyuz.iidxchartviewer.ui.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.ui.components.AutoScrollingText
import com.harroyuz.iidxchartviewer.ui.components.ChartVersionLabel
import com.harroyuz.iidxchartviewer.ui.components.CopyableText
import com.harroyuz.iidxchartviewer.ui.components.DetailStat
import com.harroyuz.iidxchartviewer.ui.motion.browseReveal
import com.harroyuz.iidxchartviewer.ui.motion.browseSharedBounds
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.NormalBlue

/** Version width belongs only to the genre row, never to the title/composer column. */
@Composable
internal fun SongInfoHeader(
    title: String,
    sourceLabel: String,
    subtitle: String,
    genre: String,
    composer: String,
    version: String,
    status: ArcadeStatus?,
    bpm: String,
    songKey: String?,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier,
    detail: Boolean = false,
    notes: String? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).browseSharedBounds(songKey?.let { "genre:$it" }, stableInDetails = true).padding(end = 10.dp)) {
                if (detail) {
                    AutoScrollingText(genre.ifBlank { "未知曲风" }, color = Muted, fontSize = 10.sp, onLongPress = { onCopyText(genre) })
                } else {
                    CopyableText(genre.ifBlank { "未知曲风" }, color = Muted, fontSize = 11.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, onCopy = onCopyText)
                }
            }
            ChartVersionLabel(
                version, status,
                Modifier.layout { measurable, constraints ->
                    // Keep some room for the genre even for exceptionally long version labels.
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = (constraints.maxWidth * .65f).toInt()))
                    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                }.browseSharedBounds(songKey?.let { "version:$it" }, stableInDetails = true),
                detail = detail,
            )
        }
        Spacer(Modifier.height(if (detail) 3.dp else 4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).browseSharedBounds(songKey?.let { "song-info:$it" }, stableInDetails = true).padding(end = if (detail) 12.dp else 10.dp)) {
                AutoScrollingText(
                    displayTitle(title, sourceLabel), color = Ink,
                    fontSize = if (detail) 27.sp else 16.sp,
                    lineHeight = if (detail) 28.sp else 22.sp,
                    fontWeight = if (detail) FontWeight.Bold else FontWeight.SemiBold,
                    onLongPress = if (detail) ({ onCopyText(title) }) else null,
                )
                if (subtitle.isNotBlank()) {
                    if (detail) AutoScrollingText(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 13.sp)
                    else Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                if (detail) {
                    AutoScrollingText(composer.ifBlank { "未知曲师" }, color = Muted, fontSize = 13.sp, onLongPress = { onCopyText(composer) })
                } else {
                    Text(composer.ifBlank { "未知曲师" }, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // Reserve the same stats width in both details so revealing NOTES cannot squeeze the title.
            Column(Modifier.widthIn(min = if (detail) 72.dp else 0.dp), horizontalAlignment = Alignment.End) {
                val bpmModifier = Modifier.browseSharedBounds(songKey?.let { "bpm:$it" }, stableInDetails = true)
                if (detail) DetailStat("BPM ", bpm.ifBlank { "—" }, bpmModifier)
                else Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = Muted)) { append("BPM ") }
                    withStyle(SpanStyle(color = NormalBlue)) { append(bpm.ifBlank { "—" }) }
                }, modifier = bpmModifier, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (notes != null) DetailStat("NOTES ", notes, Modifier.browseReveal())
            }
        }
    }
}
