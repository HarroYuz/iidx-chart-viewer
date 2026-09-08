package com.harroyuz.iidxchartviewer.ui.player

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.harroyuz.iidxchartviewer.domain.player.PLAYER_GREEN_NUMBER_MAX
import com.harroyuz.iidxchartviewer.domain.player.PLAYER_GREEN_NUMBER_MIN
import com.harroyuz.iidxchartviewer.domain.player.PLAYER_SPEED_MODE_FLOATING
import com.harroyuz.iidxchartviewer.domain.player.PLAYER_SPEED_MODE_HI
import com.harroyuz.iidxchartviewer.domain.player.PlayerSettings
import com.harroyuz.iidxchartviewer.ui.components.optionAbbreviation
import com.harroyuz.iidxchartviewer.ui.theme.Background
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.Panel
import com.harroyuz.iidxchartviewer.ui.theme.Purple

@Composable
internal fun PlayerConfigBox(
    settings: PlayerSettings,
    isSp: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSettingsChange: (PlayerSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val isFloating = settings.safeSpeedMode == PLAYER_SPEED_MODE_FLOATING
    val activeSpeedValue = if (isFloating) settings.safeGreenNumber else settings.safeSpeed
    val summary = buildString {
        if (isFloating) append("FHS $activeSpeedValue")
        else append("Hi-Speed ${activeSpeedValue}x")
        if (isSp) {
            append(", ${settings.side}")
            settings.safePlayOption.optionAbbreviation()
                .takeIf { settings.safePlayOption != "NONE" }
                ?.let { append(", $it") }
        } else {
            val options = listOf(settings.safePlayOption1P, settings.safePlayOption2P)
                .map { it.optionAbbreviation() }
            if (options.any { it != "NON" }) append(", ${options.joinToString("/")}")
        }
        if (settings.keepSpeedAcrossBpm) append(", Fixed-Speed")
        if (!isSp && settings.flip) append(", FLIP")
    }
    var speedInput by remember(settings.safeSpeed, settings.safeSpeedMode, settings.safeGreenNumber) {
        mutableStateOf(activeSpeedValue.toString())
    }
    Column(
        modifier.fillMaxWidth()
            .clip(shape)
            .background(Panel)
            .border(1.dp, ComposeColor(0xFFD8D6E1), shape),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(if (expanded) "播放器配置" else summary, color = if (expanded) Ink else Muted, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            if (expanded) {
                Text("收起", color = Muted, fontSize = 10.sp)
                Spacer(Modifier.width(4.dp))
                Text("▼", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("设置", color = Muted, fontSize = 10.sp)
                Spacer(Modifier.width(4.dp))
                Text("▲", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (expanded) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("流速:", color = Muted, fontSize = 11.sp, modifier = Modifier.width(34.dp))
                PlayerSpeedModeChoice(
                    label = "Floating Hi-Speed",
                    selected = isFloating,
                    onClick = { onSettingsChange(settings.copy(speedMode = PLAYER_SPEED_MODE_FLOATING)) },
                )
                Spacer(Modifier.width(3.dp))
                PlayerSpeedModeChoice(
                    label = "Hi-Speed",
                    selected = !isFloating,
                    onClick = { onSettingsChange(settings.copy(speedMode = PLAYER_SPEED_MODE_HI)) },
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        onSettingsChange(
                            if (isFloating) {
                                settings.copy(greenNumber = (settings.safeGreenNumber - 1).coerceAtLeast(PLAYER_GREEN_NUMBER_MIN))
                            } else {
                                settings.copy(speed = (settings.safeSpeed - 1).coerceAtLeast(1))
                            },
                        )
                    },
                    modifier = Modifier.size(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("−", color = Purple, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                BasicTextField(
                    value = speedInput,
                    onValueChange = { value ->
                        val digits = value.filter(Char::isDigit).take(if (isFloating) 4 else 3)
                        speedInput = digits
                        digits.toIntOrNull()?.let { next ->
                            if (isFloating) {
                                if (next in PLAYER_GREEN_NUMBER_MIN..PLAYER_GREEN_NUMBER_MAX) {
                                    onSettingsChange(settings.copy(greenNumber = next))
                                }
                            } else {
                                onSettingsChange(settings.copy(speed = next.coerceIn(1, 100)))
                            }
                        }
                    },
                    modifier = Modifier.width(46.dp).height(34.dp)
                        .border(1.dp, ComposeColor(0xFFB7B4C3), RoundedCornerShape(5.dp))
                        .padding(horizontal = 4.dp),
                    textStyle = TextStyle(color = Ink, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            innerTextField()
                        }
                    },
                )
                if (!isFloating) {
                    Text("x", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 2.dp))
                }
                TextButton(
                    onClick = {
                        onSettingsChange(
                            if (isFloating) {
                                settings.copy(greenNumber = (settings.safeGreenNumber + 1).coerceAtMost(PLAYER_GREEN_NUMBER_MAX))
                            } else {
                                settings.copy(speed = (settings.safeSpeed + 1).coerceAtMost(100))
                            },
                        )
                    },
                    modifier = Modifier.size(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("+", color = Purple, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerSwitchSetting("流速不随BPM变化", settings.keepSpeedAcrossBpm) {
                    onSettingsChange(settings.copy(keepSpeedAcrossBpm = it))
                }
            }
            if (!isSp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerSwitchSetting("FLIP", settings.flip) { onSettingsChange(settings.copy(flip = it)) }
                }
            }
            if (isSp) {
                PlayerSettingChoiceRow(
                    label = "位置",
                    choices = listOf("1P", "2P"),
                    selected = settings.side,
                    onSelect = { onSettingsChange(settings.copy(side = it)) },
                )
            }
            if (isSp) {
                PlayerSettingChoiceRow(
                    label = "选项",
                    choices = listOf("无", "MIRROR", "RANDOM"),
                    selected = when (settings.safePlayOption) {
                        "MIRROR" -> "MIRROR"
                        "RANDOM" -> "RANDOM"
                        else -> "无"
                    },
                    onSelect = { selected ->
                        onSettingsChange(settings.copy(playOption = if (selected == "无") "NONE" else selected))
                    },
                )
                if (settings.safePlayOption == "RANDOM") {
                    RandomMappingRow(
                        label = "",
                        mapping = if (settings.side == "1P") settings.safeRandomMapping1P else settings.safeRandomMapping2P,
                        onMappingChange = { mapping ->
                            onSettingsChange(
                                if (settings.side == "1P") settings.copy(randomMapping1P = mapping)
                                else settings.copy(randomMapping2P = mapping),
                            )
                        },
                    )
                }
            } else {
                PlayerSettingChoiceRow(
                    label = "1P",
                    choices = listOf("无", "MIRROR", "RANDOM"),
                    selected = when (settings.safePlayOption1P) {
                        "MIRROR" -> "MIRROR"
                        "RANDOM" -> "RANDOM"
                        else -> "无"
                    },
                    onSelect = { selected ->
                        onSettingsChange(settings.copy(playOption1P = if (selected == "无") "NONE" else selected))
                    },
                )
                if (settings.safePlayOption1P == "RANDOM") {
                    RandomMappingRow(
                        label = "",
                        mapping = settings.safeRandomMapping1P,
                        onMappingChange = { onSettingsChange(settings.copy(randomMapping1P = it)) },
                    )
                }
                PlayerSettingChoiceRow(
                    label = "2P",
                    choices = listOf("无", "MIRROR", "RANDOM"),
                    selected = when (settings.safePlayOption2P) {
                        "MIRROR" -> "MIRROR"
                        "RANDOM" -> "RANDOM"
                        else -> "无"
                    },
                    onSelect = { selected ->
                        onSettingsChange(settings.copy(playOption2P = if (selected == "无") "NONE" else selected))
                    },
                )
                if (settings.safePlayOption2P == "RANDOM") {
                    RandomMappingRow(
                        label = "",
                        mapping = settings.safeRandomMapping2P,
                        onMappingChange = { onSettingsChange(settings.copy(randomMapping2P = it)) },
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerSwitchSetting("小节线", settings.showBarLines) { onSettingsChange(settings.copy(showBarLines = it)) }
                PlayerSwitchSetting("小节序号", settings.showMeasureNumbers) { onSettingsChange(settings.copy(showMeasureNumbers = it)) }
                PlayerSwitchSetting("变速线", settings.showBpmChanges) { onSettingsChange(settings.copy(showBpmChanges = it)) }
            }
        }
    }
}

@Composable
private fun PlayerSpeedModeChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        Modifier
            .height(28.dp)
            .clip(shape)
            .background(if (selected) Purple.copy(alpha = .13f) else Background)
            .border(1.dp, if (selected) Purple else ComposeColor(0xFFCAC7D6), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Purple else Muted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayerSettingChoiceRow(
    label: String,
    choices: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Muted, fontSize = 11.sp, modifier = Modifier.width(48.dp))
        choices.forEach { choice ->
            PlayerChoice(
                label = choice,
                selected = choice == selected,
                onClick = { onSelect(choice) },
                modifier = Modifier.padding(end = 6.dp),
            )
        }
    }
}

@Composable
private fun PlayerChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Purple.copy(alpha = .13f) else Background)
            .border(1.dp, if (selected) Purple else ComposeColor(0xFFCAC7D6), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Purple else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlayerSwitchSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 11.sp, maxLines = 1)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 1.dp).scale(.72f),
        )
    }
}

@Composable
private fun RandomMappingRow(
    label: String,
    mapping: List<Int>,
    onMappingChange: (List<Int>) -> Unit,
) {
    val dragThreshold = with(LocalDensity.current) { 30.dp.toPx() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(if (label.isBlank()) 46.dp else 32.dp)) {
                if (label.isNotBlank()) {
                    Text(label, color = Muted, fontSize = 10.sp, lineHeight = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("拖动调整", color = Muted, fontSize = if (label.isBlank()) 10.sp else 7.sp, lineHeight = if (label.isBlank()) 10.sp else 8.sp, maxLines = 1)
            }
            Spacer(Modifier.width(6.dp))
            mapping.forEachIndexed { index, value ->
                RandomLaneButton(
                    value = value,
                    dragThreshold = dragThreshold,
                    onDragSwap = { shift ->
                        val target = (index + shift).coerceIn(0, mapping.lastIndex)
                        if (target != index) {
                            val updated = mapping.toMutableList()
                            val moved = updated[index]
                            updated[index] = updated[target]
                            updated[target] = moved
                            onMappingChange(updated)
                        }
                    },
                    modifier = Modifier.padding(end = 2.dp),
                )
            }
            RandomLaneButton(
                value = null,
                dragThreshold = dragThreshold,
                onRandomize = { onMappingChange((1..7).shuffled()) },
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun RandomLaneButton(
    value: Int?,
    dragThreshold: Float,
    onDragSwap: (Int) -> Unit = {},
    onRandomize: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var dragDistance by remember { mutableStateOf(0f) }
    val buttonShape = RoundedCornerShape(5.dp)
    val labelTextSize = with(LocalDensity.current) { if (value == null) 9.sp.toPx() else 13.sp.toPx() }
    Box(
        modifier
            .graphicsLayer {
                if (value != null) {
                    translationX = dragDistance
                    alpha = if (dragDistance == 0f) 1f else .5f
                }
            }
            .zIndex(if (value != null && dragDistance != 0f) 1f else 0f)
            .size(28.dp)
            .clip(buttonShape)
            .background(if (value == null) Panel else if (value % 2 == 1) ComposeColor.White else ComposeColor(0xFF252535))
            .border(1.dp, ComposeColor(0xFF11131A), buttonShape)
            .then(
                if (value == null) Modifier.clickable(onClick = onRandomize)
                else Modifier.pointerInput(value, dragThreshold) {
                    detectDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onDragCancel = { dragDistance = 0f },
                        onDragEnd = {
                            val shift = kotlin.math.round(dragDistance / dragThreshold).toInt()
                            dragDistance = 0f
                            if (shift != 0) onDragSwap(shift)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount.x
                        },
                    )
                },
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = labelTextSize
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    color = if (value == null) Purple.toArgb() else ComposeColor(0xFFE33D4F).toArgb()
                    style = if (value == null) Paint.Style.FILL else Paint.Style.STROKE
                    strokeWidth = if (value == null) 0f else 2.2f
                }
                val baseline = (size.height - (paint.ascent() + paint.descent())) / 2f
                val text = value?.toString() ?: "随机"
                canvas.nativeCanvas.drawText(text, size.width / 2f, baseline, paint)
                if (value != null) {
                    paint.style = Paint.Style.FILL
                    paint.color = ComposeColor(0xFFFFD23F).toArgb()
                    canvas.nativeCanvas.drawText(text, size.width / 2f, baseline, paint)
                }
            }
        }
    }
}
