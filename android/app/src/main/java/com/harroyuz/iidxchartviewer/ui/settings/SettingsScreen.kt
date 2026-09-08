package com.harroyuz.iidxchartviewer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.BuildConfig
import com.harroyuz.iidxchartviewer.domain.model.TextageSyncProgress
import com.harroyuz.iidxchartviewer.domain.sync.DataSyncTarget
import com.harroyuz.iidxchartviewer.ui.components.AppTopBar
import com.harroyuz.iidxchartviewer.ui.components.DataSyncProgressBanner
import com.harroyuz.iidxchartviewer.ui.components.TextageSyncBanner
import com.harroyuz.iidxchartviewer.ui.history.formatBjmHistoryTime
import com.harroyuz.iidxchartviewer.ui.theme.Background
import com.harroyuz.iidxchartviewer.ui.theme.CardSurface
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.Orange
import com.harroyuz.iidxchartviewer.ui.theme.Outline
import com.harroyuz.iidxchartviewer.ui.theme.Purple

private fun formatDataSourceLastSync(value: Long): String =
    if (value <= 0L) {
        "最后更新：未同步"
    } else {
        "最后更新：${formatBjmHistoryTime(value)}"
    }

@Composable
internal fun UpdateSettingsScreen(
    visualEffectsDisabled: Boolean,
    onVisualEffectsDisabledChange: (Boolean) -> Unit,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenMenu: () -> Unit,
    textageProgress: TextageSyncProgress?,
    textageError: String?,
    textageLastSyncAt: Long,
    bjmMusicLastSyncAt: Long,
    bjmScoresLastSyncAt: Long,
    syncTarget: DataSyncTarget?,
    syncStage: String?,
    syncProgress: Float?,
    bjmLoggedIn: Boolean,
    onFullDataSync: () -> Unit,
    onSyncTextage: () -> Unit,
    onSyncBjmMusic: () -> Unit,
    onSyncBjmScores: () -> Unit,
    onClearChartCache: () -> Unit,
) {
    val syncing = syncTarget != null

    Column(Modifier.fillMaxSize().background(Background)) {
        AppTopBar(title = "设置", onNavigate = onOpenMenu, menu = true)
        HorizontalDivider(color = Outline)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(16.dp).clip(MaterialTheme.shapes.large).background(CardSurface),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("自动检查更新", color = Ink, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("每天检查 GitHub Release 是否有新版本", color = Muted, fontSize = 12.sp)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            HorizontalDivider(color = Outline)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("禁用视觉效果", color = Ink, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("关闭曲目和谱面页面的展开动画，直接切换页面", color = Muted, fontSize = 12.sp)
                }
                Switch(checked = visualEffectsDisabled, onCheckedChange = onVisualEffectsDisabledChange)
            }
            HorizontalDivider(color = Outline)
            SettingsActionRow(
                title = "全量数据同步",
                subtitle = "按顺序同步 Textage、BJM 曲目库和用户成绩",
                actionLabel = if (syncTarget == DataSyncTarget.FULL) "同步中…" else "同步",
                enabled = !syncing,
                onClick = onFullDataSync,
            )
            if (!syncStage.isNullOrBlank()) {
                Text(
                    syncStage,
                    color = Purple,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                )
            }
            HorizontalDivider(color = Outline)
            Text(
                "数据源同步",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 2.dp),
            )
            SettingsActionRow(
                title = "Textage 曲目库",
                lastUpdatedAt = textageLastSyncAt,
                actionLabel = if (syncTarget == DataSyncTarget.TEXTAGE) "同步中…" else "同步",
                enabled = !syncing,
                onClick = onSyncTextage,
            )
            SettingsActionRow(
                title = "BJM 曲目库",
                lastUpdatedAt = bjmMusicLastSyncAt,
                actionLabel = if (syncTarget == DataSyncTarget.BJM_MUSIC) "同步中…" else "同步",
                enabled = !syncing,
                onClick = onSyncBjmMusic,
            )
            SettingsActionRow(
                title = "用户成绩库",
                lastUpdatedAt = bjmScoresLastSyncAt,
                actionLabel = when {
                    syncTarget == DataSyncTarget.BJM_SCORES -> "同步中…"
                    !bjmLoggedIn -> "需登录"
                    else -> "同步"
                },
                enabled = !syncing && bjmLoggedIn,
                onClick = onSyncBjmScores,
            )
            HorizontalDivider(color = Outline)
            SettingsActionRow(
                title = "清除谱面缓存",
                subtitle = "下次打开谱面时重新解析",
                actionLabel = "清除",
                enabled = !syncing,
                onClick = onClearChartCache,
            )
            if (syncTarget == DataSyncTarget.FULL && syncProgress != null) {
                DataSyncProgressBanner(syncStage, syncProgress)
            } else if (textageProgress != null) {
                TextageSyncBanner(textageProgress)
            }
            Text(
                "IIDX Data  ·  ${BuildConfig.VERSION_NAME}",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                textAlign = TextAlign.Center,
            )
            if (!textageError.isNullOrBlank()) {
                Text(
                    textageError,
                    color = Orange,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String? = null,
    lastUpdatedAt: Long? = null,
    actionLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, style = MaterialTheme.typography.titleMedium)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            if (lastUpdatedAt != null) {
                Spacer(Modifier.height(2.dp))
                Text(formatDataSourceLastSync(lastUpdatedAt), color = Muted, fontSize = 11.sp)
            }
        }
        TextButton(onClick = onClick, enabled = enabled) {
            Text(actionLabel, color = if (enabled) Purple else Muted)
        }
    }
}
