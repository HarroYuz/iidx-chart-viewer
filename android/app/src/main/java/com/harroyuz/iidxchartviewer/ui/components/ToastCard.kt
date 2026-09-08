package com.harroyuz.iidxchartviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun ToastCard(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        delay(2_200L)
        onDismiss()
    }
    Surface(
        modifier.padding(14.dp).clickable { onDismiss() },
        color = ComposeColor(0xFF303038),
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 4.dp,
    ) {
        Text(message, color = ComposeColor.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
    }
}
