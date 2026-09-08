package com.harroyuz.iidxchartviewer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.BuildConfig
import com.harroyuz.iidxchartviewer.data.remote.update.GithubReleaseInfo
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.Purple

@Composable
internal fun UpdateDialog(
    release: GithubReleaseInfo,
    downloadProgress: Float?,
    installing: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    val busy = downloadProgress != null || installing
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("发现新版本") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "v${BuildConfig.VERSION_NAME.removePrefix("v")} → v${release.tagName.removePrefix("v")}",
                    color = Purple,
                    fontWeight = FontWeight.Bold,
                )
                Text(release.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(release.notes.ifBlank { "暂无 Release Note" }, color = Muted, fontSize = 12.sp)
                if (downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        if (downloadProgress <= 0f) "正在准备下载…" else "正在下载 ${(downloadProgress * 100).toInt()}%",
                        color = Muted,
                        fontSize = 11.sp,
                    )
                } else if (installing) {
                    Text("正在启动安装…", color = Muted, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload, enabled = !busy) {
                Text(if (installing) "安装中…" else "更新")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("稍后") }
        },
    )
}
