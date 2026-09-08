package com.harroyuz.iidxchartviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.TextageSyncProgress
import com.harroyuz.iidxchartviewer.ui.theme.Cyan
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.NormalBlue
import com.harroyuz.iidxchartviewer.ui.theme.Orange
import com.harroyuz.iidxchartviewer.ui.theme.Panel
import com.harroyuz.iidxchartviewer.ui.theme.Purple

@Composable
internal fun LocalDataLoadingScreen(
    progress: Float,
    stage: String,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("正在加载本地数据", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(stage, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(20.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = Purple,
            trackColor = Panel,
        )
        Spacer(Modifier.height(8.dp))
        Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", color = NormalBlue, fontSize = 12.sp)
    }
}

@Composable
internal fun TextageBootstrapScreen(
    progress: TextageSyncProgress?,
    error: String?,
    retryEnabled: Boolean,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (error == null) "正在加载谱面元数据" else "谱面元数据获取失败", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            if (error == null) "首次启动会从 Textage 获取曲目元数据并保存到本机。" else error,
            color = if (error == null) Muted else Orange,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(22.dp))

        if (progress == null) {
            CircularProgressIndicator(color = Purple)
        } else {
            Text(
                if (progress.total == 0) "正在检查本地谱面缓存…" else "正在获取：${progress.currentTitle}",
                color = Cyan,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).background(ComposeColor(0xFF26283B))) {
                Box(
                    Modifier.fillMaxWidth(progress.fraction).height(8.dp)
                        .background(ComposeColor(0xFFA88CFF)),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("${progress.completed} / ${progress.total}", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (progress.failed > 0) {
                Spacer(Modifier.height(5.dp))
                Text("失败 ${progress.failed} 张，将在下次启动继续重试", color = Orange, fontSize = 11.sp)
            }
        }

        if (error != null) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onRetry, enabled = retryEnabled) { Text(if (retryEnabled) "重试" else "重试中") }
        }
    }
}

@Composable
internal fun TextageSyncBanner(progress: TextageSyncProgress) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("正在同步谱面数据", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${progress.completed}/${progress.total}", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)).background(ComposeColor(0xFF26283B))) {
            Box(Modifier.fillMaxWidth(progress.fraction).height(4.dp).background(Purple))
        }
        Text(
            "当前：${progress.currentTitle}${if (progress.failed > 0) " · 失败 ${progress.failed} 张" else ""}",
            color = Muted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
internal fun DataSyncProgressBanner(stage: String?, progress: Float) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stage ?: "正在同步数据", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${(safeProgress * 100).toInt()}%", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)).background(ComposeColor(0xFF26283B))) {
            Box(Modifier.fillMaxWidth(safeProgress).height(4.dp).background(Purple))
        }
    }
}

@Composable
internal fun ChartLoadError(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("谱面数据获取失败", color = Orange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("请检查网络后重试", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("重试") }
    }
}

@Composable
internal fun ChartParseWarning(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("已获取页面，但暂未识别谱面节点", color = Orange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("重新解析") }
    }
}
