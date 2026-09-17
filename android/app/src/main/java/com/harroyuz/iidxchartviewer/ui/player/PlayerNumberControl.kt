package com.harroyuz.iidxchartviewer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.ui.theme.*

internal enum class PlayerNumber { SPEED, HEIGHT }

/** Keeping the gesture owner composed lets the drag continue while its panel is invisible. */
@Composable
internal fun PlayerNumberControl(
    label: String,
    value: String,
    decimal: Boolean,
    onValue: (String) -> Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onRotaryStart: (Offset) -> Unit,
    onRotaryMove: (Offset) -> Unit,
    onRotaryEnd: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(Offset.Zero) }
    val start by rememberUpdatedState(onRotaryStart)
    val move by rememberUpdatedState(onRotaryMove)
    val end by rememberUpdatedState(onRotaryEnd)
    TextButton(onClick = onDecrease, modifier = Modifier.size(32.dp), contentPadding = PaddingValues(0.dp)) {
        Text("−", color = Purple, fontSize = 20.sp)
    }
    Box(
        Modifier.width(60.dp).height(32.dp)
            .border(1.dp, Muted.copy(alpha = .45f), RoundedCornerShape(5.dp))
            .background(Background, RoundedCornerShape(5.dp))
            .onGloballyPositioned { position = it.positionInRoot() }
            .semantics { contentDescription = "$label，点击输入，长按旋转调节" }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { start(position + it) },
                    onDrag = { change, _ -> change.consume(); move(position + change.position) },
                    onDragEnd = { end() },
                    onDragCancel = { end() },
                )
            }
            .clickable { input = value; invalid = false; editing = true },
        contentAlignment = Alignment.Center,
    ) { Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    TextButton(onClick = onIncrease, modifier = Modifier.size(32.dp), contentPadding = PaddingValues(0.dp)) {
        Text("+", color = Purple, fontSize = 20.sp)
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(label) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { next ->
                        if (next.length <= 7 && next.matches(if (decimal) Regex("[0-9]*(\\.[0-9]{0,2})?") else Regex("[0-9]*"))) {
                            input = next; invalid = false
                        }
                    },
                    singleLine = true,
                    isError = invalid,
                    supportingText = { if (invalid) Text("请输入有效范围内的数值") },
                    keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
                )
            },
            confirmButton = { TextButton(onClick = { if (onValue(input)) editing = false else invalid = true }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("取消") } },
        )
    }
}
