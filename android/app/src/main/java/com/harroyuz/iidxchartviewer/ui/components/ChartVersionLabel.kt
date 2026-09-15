package com.harroyuz.iidxchartviewer.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.NormalBlue
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus

internal val RemovedSongColor = Color(0xFFAD3636)

@Composable
internal fun ChartVersionLabel(
    version: String,
    status: ArcadeStatus?,
    modifier: Modifier = Modifier,
    detail: Boolean = false,
) {
    Text(
        buildAnnotatedString {
            if (detail) withStyle(SpanStyle(color = Muted)) { append("版本 ") }
            withStyle(SpanStyle(color = NormalBlue)) { append(version.ifBlank { "—" }) }
            if (status?.isUnavailable == true) {
                withStyle(SpanStyle(color = RemovedSongColor)) { append(" · 删除曲") }
            }
        },
        modifier = modifier,
        fontSize = if (detail) 10.sp else 11.sp,
        maxLines = 1,
        softWrap = false,
    )
}
