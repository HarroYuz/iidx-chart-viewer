package com.harroyuz.iidxchartviewer.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.NormalBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private data class StrokedTextMetrics(
    val width: Float,
    val height: Float,
    val baseline: Float,
)

@Composable
internal fun StrokedText(
    text: String,
    fillColor: ComposeColor,
    fontSize: TextUnit,
    fontWeight: FontWeight? = null,
    strokeColor: ComposeColor = Ink,
    strokeWidth: Float = 0.8f,
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { fontSize.toPx() }
    val metrics = remember(text, textSizePx, fontWeight) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            typeface = Typeface.create(
                Typeface.DEFAULT,
                if ((fontWeight?.weight ?: FontWeight.Normal.weight) >= FontWeight.Bold.weight) Typeface.BOLD else Typeface.NORMAL,
            )
        }
        val fontMetrics = paint.fontMetrics
        StrokedTextMetrics(
            width = paint.measureText(text),
            height = fontMetrics.bottom - fontMetrics.top,
            baseline = -fontMetrics.top,
        )
    }
    val strokePx = with(density) { strokeWidth.dp.toPx() }
    Canvas(
        Modifier
            .width(with(density) { (metrics.width + strokePx * 2f).toDp() })
            .height(with(density) { (metrics.height + strokePx * 2f).toDp() }),
    ) {
        drawIntoCanvas { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = textSizePx
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    if ((fontWeight?.weight ?: FontWeight.Normal.weight) >= FontWeight.Bold.weight) Typeface.BOLD else Typeface.NORMAL,
                )
                this.strokeWidth = strokePx
                this.strokeJoin = Paint.Join.ROUND
            }
            val x = strokePx
            val y = strokePx + metrics.baseline
            paint.style = Paint.Style.STROKE
            paint.color = strokeColor.toArgb()
            canvas.nativeCanvas.drawText(text, x, y, paint)
            paint.style = Paint.Style.FILL
            paint.color = fillColor.toArgb()
            canvas.nativeCanvas.drawText(text, x, y, paint)
        }
    }
}

@Composable
internal fun CopyableText(
    text: String,
    color: ComposeColor,
    fontSize: TextUnit,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    onCopy: (String) -> Unit,
) {
    Text(
        text,
        color = color,
        fontSize = fontSize,
        maxLines = maxLines,
        overflow = overflow,
        modifier = Modifier.pointerInput(text) {
            detectTapGestures(onLongPress = { onCopy(text) })
        },
    )
}

@Composable
internal fun AutoScrollingText(
    text: String,
    color: ComposeColor,
    fontSize: TextUnit,
    lineHeight: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(text, scrollState.maxValue) {
        if (scrollState.maxValue <= 0) return@LaunchedEffect
        scrollState.scrollTo(0)
        val durationMillis = (scrollState.maxValue * 7).coerceIn(1_200, 5_000)
        delay(900L)
        while (isActive) {
            scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
            )
            delay(900L)
            scrollState.animateScrollTo(
                0,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
            )
            delay(900L)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (onLongPress == null) {
                    Modifier
                } else {
                    Modifier.pointerInput(text) {
                        detectTapGestures(onLongPress = { onLongPress() })
                    }
                },
            )
            .horizontalScroll(scrollState, enabled = false),
    ) {
        Text(
            text,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun DetailStat(label: String, value: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = Muted)) { append(label) }
            withStyle(SpanStyle(color = NormalBlue)) { append(value) }
        },
        fontSize = 10.sp,
        maxLines = 1,
        softWrap = false,
    )
}
