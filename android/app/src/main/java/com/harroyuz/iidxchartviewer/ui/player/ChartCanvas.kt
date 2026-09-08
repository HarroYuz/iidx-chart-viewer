package com.harroyuz.iidxchartviewer.ui.player

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.State
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.TextageChartData
import com.harroyuz.iidxchartviewer.domain.player.dpOptionSourceLane
import com.harroyuz.iidxchartviewer.domain.player.dpDisplayLane
import com.harroyuz.iidxchartviewer.domain.player.playerPixelsPerBeat
import com.harroyuz.iidxchartviewer.ui.theme.PlayerBackground
import com.harroyuz.iidxchartviewer.ui.theme.PlayerBpmGreen
import com.harroyuz.iidxchartviewer.ui.theme.PlayerCenterGap
import com.harroyuz.iidxchartviewer.ui.theme.PlayerEdge
import com.harroyuz.iidxchartviewer.ui.theme.PlayerLane
import com.harroyuz.iidxchartviewer.ui.theme.PlayerMeasureText
import com.harroyuz.iidxchartviewer.ui.theme.PlayerRed
import com.harroyuz.iidxchartviewer.ui.theme.PlayerSkyBlue

@Composable
internal fun ChartCanvas(
    data: TextageChartData,
    currentBeatState: State<Float>,
    speed: Int,
    speedMode: String,
    greenNumber: Int,
    keepSpeedAcrossBpm: Boolean,
    showBarLines: Boolean,
    showBpmChanges: Boolean,
    showMeasureNumbers: Boolean,
    side: String,
    flip: Boolean,
    playOption: String,
    playOption1P: String,
    playOption2P: String,
    randomMapping1P: List<Int>,
    randomMapping2P: List<Int>,
    playing: Boolean,
    onCurrentBeatChange: (Float) -> Unit,
    modifier: Modifier,
) {
    val laneCount = if (data.chart.mode == "DP") 16 else 8
    var canvasHeightPx by remember { mutableStateOf(0f) }
    val fallbackHeightPx = with(LocalDensity.current) { 360.dp.toPx() }
    val judgeDistancePx = (canvasHeightPx.takeIf { it > 0f } ?: fallbackHeightPx) * .92f
    // Keep note geometry in musical beat coordinates. Hi-Speed uses the
    // existing BPM-dependent beat spacing. Floating Hi-Speed derives the
    // spacing from the starting BPM so the selected green number represents
    // greenNumber / 600 seconds from the top of the lane to the judge line.
    val pixelsPerBeat = playerPixelsPerBeat(
        speedMode = speedMode,
        speed = speed,
        greenNumber = greenNumber,
        initialBpm = data.bpmAt(0f),
        judgeDistancePx = judgeDistancePx,
    )
    val pixelsPerSecond = pixelsPerBeat * data.bpmAt(0f).coerceAtLeast(1f) / 60f
    val labelTextSize = with(LocalDensity.current) { 10.sp.toPx() }
    val labelPadding = with(LocalDensity.current) { 4.dp.toPx() }
    val latestPlaying by androidx.compose.runtime.rememberUpdatedState(playing)
    val latestOnCurrentBeatChange by androidx.compose.runtime.rememberUpdatedState(onCurrentBeatChange)
    val isSp = data.chart.mode != "DP"
    val measureAlign = if (isSp && side == "2P") Paint.Align.RIGHT else Paint.Align.LEFT
    val bpmAlign = if (isSp && side == "2P") Paint.Align.LEFT else Paint.Align.RIGHT
    val measurePaint = remember(labelTextSize, measureAlign) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PlayerMeasureText.toArgb()
            textSize = labelTextSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = measureAlign
        }
    }
    val bpmPaint = remember(labelTextSize, bpmAlign) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PlayerBpmGreen.toArgb()
            textSize = labelTextSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = bpmAlign
        }
    }
    Canvas(
        modifier
            .onSizeChanged { canvasHeightPx = it.height.toFloat() }
            .pointerInput(data.chart.id, speed, speedMode, greenNumber, keepSpeedAcrossBpm) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (!latestPlaying) {
                        if (keepSpeedAcrossBpm) {
                            latestOnCurrentBeatChange(
                                data.beatAtSeconds(data.secondsAtBeat(currentBeatState.value) + dragAmount / pixelsPerSecond),
                            )
                        } else {
                            latestOnCurrentBeatChange(currentBeatState.value + dragAmount / pixelsPerBeat)
                        }
                    }
                }
        },
    ) {
        // Read the rapidly changing playback state in the draw phase. This
        // keeps the surrounding player controls out of the per-frame
        // recomposition path.
        val currentBeat = currentBeatState.value
        val currentSeconds = data.secondsAtBeat(currentBeat)
        fun pixelsFromCurrentBeat(beat: Float): Float = if (keepSpeedAcrossBpm) {
            (data.secondsAtBeat(beat) - currentSeconds) * pixelsPerSecond
        } else {
            (beat - currentBeat) * pixelsPerBeat
        }
        val dpGapUnits = 1.5f
        // SP has a gray information column opposite the scratch column. It
        // uses the same 1.5-note width as the DP center gap.
        val laneWidths = if (isSp) {
            listOf(1.5f) + List(7) { 1f } + listOf(1.5f)
        } else listOf(1.5f) + List(7) { 1f } + List(7) { 1f } + listOf(1.5f)
        val unit = if (isSp) size.width / laneWidths.sum() else size.width / (laneWidths.sum() + dpGapUnits)
        val laneLefts = laneWidths.runningFold(0f) { sum, width -> sum + width * unit }.dropLast(1)
        val spLaneOffset = if (isSp && side == "2P") 1 else 0
        val dpLeftWidth = laneWidths.take(8).sum()
        fun dpLaneStart(lane: Int): Float = if (lane < 8) {
            laneWidths.take(lane).sum() * unit
        } else {
            (dpLeftWidth + dpGapUnits + laneWidths.slice(8 until lane).sum()) * unit
        }
        fun dpBoundaryX(boundary: Int): Float = if (boundary <= 8) {
            laneWidths.take(boundary).sum() * unit
        } else {
            (dpLeftWidth + dpGapUnits + laneWidths.slice(8 until boundary).sum()) * unit
        }
        val judgeY = size.height * .92f

        drawRect(PlayerBackground)
        if (isSp) {
            val grayStart = if (side == "1P") laneWidths.take(8).sum() * unit else 0f
            drawRect(
                color = PlayerCenterGap,
                topLeft = Offset(grayStart, 0f),
                size = Size(1.5f * unit, size.height),
            )
        } else {
            drawRect(
                color = PlayerCenterGap,
                topLeft = Offset(dpLeftWidth * unit, 0f),
                size = Size(dpGapUnits * unit, size.height),
            )
        }
        if (isSp) {
            for (boundary in 0..laneWidths.size) {
                val x = laneLefts.getOrNull(boundary) ?: size.width
                drawLine(
                    color = if (boundary == 0 || boundary == laneWidths.size) PlayerEdge else PlayerLane,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (boundary == 0 || boundary == laneWidths.size) 2f else 1f,
                )
            }
        } else {
            for (lane in 0..laneCount) {
                val x = dpBoundaryX(lane)
                drawLine(
                    color = if (lane == 0 || lane == laneCount) PlayerEdge else PlayerLane,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (lane == 0 || lane == laneCount) 2f else 1f,
                )
            }
        }
        if (!isSp) {
            drawLine(
                color = PlayerEdge,
                start = Offset((dpLeftWidth + dpGapUnits) * unit, 0f),
                end = Offset((dpLeftWidth + dpGapUnits) * unit, size.height),
                strokeWidth = 2f,
            )
        }
        val infoStartX = if (isSp) {
            if (side == "1P") laneWidths.take(8).sum() * unit else 0f
        } else {
            dpLeftWidth * unit
        }
        val infoEndX = if (isSp) {
            infoStartX + 1.5f * unit
        } else {
            infoStartX + dpGapUnits * unit
        }
        val infoOnLeft = isSp && side == "2P"
        val measureX = if (infoOnLeft) infoEndX - labelPadding else infoStartX + labelPadding
        val measureAlign = if (infoOnLeft) Paint.Align.RIGHT else Paint.Align.LEFT
        val bpmX = if (infoOnLeft) infoStartX + labelPadding else infoEndX - labelPadding
        val bpmAlign = if (infoOnLeft) Paint.Align.LEFT else Paint.Align.RIGHT
        val measureBaseline = { y: Float -> (y - 4f).coerceAtLeast(labelTextSize) }
        val bpmBaseline = { y: Float -> (y - 4f).coerceAtLeast(labelTextSize) }
        val firstVisibleBeat = if (keepSpeedAcrossBpm) {
            data.beatAtSeconds((currentSeconds - size.height / pixelsPerSecond).coerceAtLeast(0f))
        } else {
            currentBeat - size.height / pixelsPerBeat
        }
        val lastVisibleBeat = if (keepSpeedAcrossBpm) {
            data.beatAtSeconds(currentSeconds + size.height / pixelsPerSecond)
        } else {
            currentBeat + size.height / pixelsPerBeat
        }
        if (showBarLines || showMeasureNumbers) {
            val firstMeasure = (
                data.measureAt(firstVisibleBeat) - 2
            ).coerceAtLeast(1)
            val lastMeasure = (
                data.measureAt(lastVisibleBeat) + 2
            ).coerceAtMost(data.measureCount())
            for (measure in firstMeasure..lastMeasure) {
                val measureBeat = data.measureStart(measure)
                val y = judgeY - pixelsFromCurrentBeat(measureBeat)
                if (y in -2f..size.height + 2f) {
                    if (showBarLines) {
                        drawLine(
                            ComposeColor(0xFF444756),
                            Offset(0f, y),
                            Offset(size.width, y),
                            strokeWidth = if (measure % 4 == 1) 3f else 2f,
                        )
                    }
                    if (showMeasureNumbers) {
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                measure.toString(),
                                measureX,
                                measureBaseline(y),
                                measurePaint,
                            )
                        }
                    }
                }
            }
        }
        if (showBpmChanges) {
            data.positiveBpmChanges.forEach { change ->
                val y = judgeY - pixelsFromCurrentBeat(change.beat)
                if (y in -labelTextSize..size.height + labelTextSize) {
                    drawLine(
                        PlayerBpmGreen,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = 2f,
                    )
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            formatPlayerBpm(change.bpm),
                            bpmX,
                            bpmBaseline(y),
                            bpmPaint,
                        )
                    }
                }
            }
        }
        drawLine(PlayerRed, Offset(0f, judgeY), Offset(size.width, judgeY), strokeWidth = 5f)
        clipRect(0f, 0f, size.width, size.height) {
            data.forEachVisibleNote(firstVisibleBeat, lastVisibleBeat) { note ->
            val noteEndBeat = note.beat + note.holdBeats
            if (noteEndBeat < currentBeat - 0.001f) return@forEachVisibleNote
            val visibleStartBeat = maxOf(note.beat, currentBeat)
            val y = if (keepSpeedAcrossBpm) {
                val visibleStartSeconds = maxOf(data.noteStartSeconds(note), currentSeconds)
                judgeY - (visibleStartSeconds - currentSeconds) * pixelsPerSecond
            } else {
                judgeY - pixelsFromCurrentBeat(visibleStartBeat)
            }
            val endY = if (keepSpeedAcrossBpm) {
                judgeY - (data.noteEndSeconds(note) - currentSeconds) * pixelsPerSecond
            } else {
                judgeY - pixelsFromCurrentBeat(noteEndBeat)
            }
            if (maxOf(y, endY) < 0f || minOf(y, endY) > size.height) return@forEachVisibleNote
            // Keep the source lane separate from the displayed lane. RANDOM
            // changes only the position; note color must remain tied to the
            // original chart lane (especially scratch vs. key colors).
            val sourceLane = note.lane
            val rawLane = if (isSp) {
                sourceLane.mod(8)
            } else {
                sourceLane.coerceIn(0, laneCount - 1)
            }
            // In DP, FLIP is applied to the complete two-side chart first.
            // The side-specific options must then be evaluated on the side
            // where the chart has landed, matching the arcade behavior.
            val optionSourceLane = if (isSp) rawLane else dpOptionSourceLane(rawLane, flip)
            val laneOption = if (isSp) playOption else if (optionSourceLane >= 8) playOption2P else playOption1P
            fun mappedKeyLane(lane: Int, mapping: List<Int>): Int = when (laneOption) {
                "MIRROR" -> 8 - lane
                "RANDOM" -> (mapping.indexOf(lane).takeIf { it >= 0 } ?: (lane - 1)) + 1
                else -> lane
            }
            val logicalLane = if (isSp && optionSourceLane > 0) {
                mappedKeyLane(optionSourceLane, if (side == "1P") randomMapping1P else randomMapping2P)
            } else rawLane
            val destinationKeyLane = when {
                isSp && optionSourceLane > 0 -> logicalLane
                !isSp && optionSourceLane in 1..7 -> mappedKeyLane(optionSourceLane, randomMapping1P)
                !isSp && optionSourceLane >= 9 -> mappedKeyLane(optionSourceLane - 8, randomMapping2P)
                else -> 0
            }
            val displayLane = when {
                isSp && side == "2P" -> if (logicalLane == 0) 7 else logicalLane - 1
                !isSp && optionSourceLane >= 8 -> dpDisplayLane(optionSourceLane, destinationKeyLane)
                !isSp && optionSourceLane in 1..7 -> destinationKeyLane
                else -> if (!isSp && flip) {
                    if (optionSourceLane == 8) 15 else optionSourceLane
                } else {
                    logicalLane
                }
            }
            val laneIndex = displayLane.coerceIn(0, laneCount - 1)
            // In SP 2P the gray information column is physically before the
            // seven keys and scratch, so shift every displayed lane by one
            // slot. The logical lane mapping remains unchanged.
            val physicalLaneIndex = if (isSp) laneIndex + spLaneOffset else laneIndex
            val laneWidth = laneWidths.getOrElse(physicalLaneIndex) { 1f } * unit
            val laneStart = if (isSp) {
                laneLefts.getOrElse(physicalLaneIndex) { 0f }
            } else {
                dpLaneStart(laneIndex)
            }
            val left = laneStart + laneWidth * .12f
            val width = laneWidth * .76f
            // Colors follow the mapped physical key. For example, source
            // lane 6 moved to lane 7 becomes white after RANDOM.
            val noteColor = if (destinationKeyLane == 0) {
                PlayerRed
            } else if (destinationKeyLane % 2 == 1) {
                ComposeColor.White
            } else {
                PlayerSkyBlue
            }
            if (note.holdBeats > 0f) {
                val holdTop = minOf(y, endY).coerceIn(0f, size.height)
                val holdBottom = maxOf(y, endY).coerceIn(0f, size.height)
                val holdHeight = holdBottom - holdTop
                val holdWidth = width * .88f
                if (holdHeight > 0f) {
                    drawRect(
                        color = noteColor.copy(alpha = .58f),
                        topLeft = Offset(left + (width - holdWidth) / 2f, holdTop),
                        size = Size(holdWidth, holdHeight),
                    )
                }
            }
            if (note.beat >= currentBeat - 0.001f && y in 0f..size.height) {
                drawRoundRect(
                    color = noteColor,
                    topLeft = Offset(left, y - 6f),
                    size = Size(width, 12f),
                    cornerRadius = CornerRadius(5f),
                )
            }
            if (note.holdBeats > 0f && endY in 0f..size.height) {
                drawRoundRect(
                    color = noteColor,
                    topLeft = Offset(left, endY - 6f),
                    size = Size(width, 12f),
                    cornerRadius = CornerRadius(5f),
                )
            }
            }
        }
    }
}
