package com.harroyuz.iidxchartviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.ui.motion.browseReveal
import com.harroyuz.iidxchartviewer.ui.theme.Ink

internal val RemovedSongColor = Color(0xFFAD3636)

internal fun songTitleColor(status: ArcadeStatus?): Color =
    if (status?.isUnavailable == true) RemovedSongColor else Ink

internal fun arcadeStatusLabel(status: ArcadeStatus): String = when (status) {
    ArcadeStatus.DELETED -> "已删除 · 当前街机版未收录"
    ArcadeStatus.CONSUMER_ONLY -> "家用版 · 当前街机版未收录"
    else -> ""
}

@Composable
internal fun ArcadeStatusNotice(status: ArcadeStatus, modifier: Modifier = Modifier) {
    if (!status.isUnavailable) return
    Column(
        modifier.fillMaxWidth().browseReveal()
            .background(RemovedSongColor.copy(alpha = .06f), MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(arcadeStatusLabel(status), color = RemovedSongColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            "BJM 数据主要对应当前收录谱面，旧版可能没有成绩或 NOTE 数不同；不符合校验的成绩不会关联，历史记录仍保留。",
            color = Ink, fontSize = 11.sp, lineHeight = 15.sp,
        )
    }
}
