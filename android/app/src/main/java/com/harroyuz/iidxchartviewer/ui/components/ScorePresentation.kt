package com.harroyuz.iidxchartviewer.ui.components

import androidx.compose.ui.graphics.Color as ComposeColor
import com.harroyuz.iidxchartviewer.domain.catalog.songGroupKey
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.score.difficultyIndex
import com.harroyuz.iidxchartviewer.domain.score.rankSummary
import com.harroyuz.iidxchartviewer.ui.theme.ClearAssist
import com.harroyuz.iidxchartviewer.ui.theme.ClearEasy
import com.harroyuz.iidxchartviewer.ui.theme.ClearExHard
import com.harroyuz.iidxchartviewer.ui.theme.ClearFailed
import com.harroyuz.iidxchartviewer.ui.theme.ClearFullCombo
import com.harroyuz.iidxchartviewer.ui.theme.ClearHard
import com.harroyuz.iidxchartviewer.ui.theme.ClearNormal
import com.harroyuz.iidxchartviewer.ui.theme.Green
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.NormalBlue

internal fun difficultyColor(value: String): ComposeColor = when (value) {
    "B" -> Green
    "N" -> NormalBlue
    "H" -> ComposeColor(0xFF9A6700)
    "A" -> ComposeColor(0xFFD04C77)
    "L" -> ComposeColor(0xFF5635B8)
    else -> Muted
}

internal fun scoreForChart(chart: IidxChart, index: BjmIndex): BjmScore? {
    val musicId = index.songMusicIds[songGroupKey(chart)] ?: return null
    return index.scoresByKey["$musicId:${if (chart.mode == "DP") 1 else 0}:${difficultyIndex(chart.difficulty)}"]
}

internal fun clearFlagShortName(value: Int): String = when (value) {
    1 -> "F"
    2 -> "AC"
    3 -> "EC"
    4 -> "NC"
    5 -> "HC"
    6 -> "EXC"
    7 -> "FC"
    else -> ""
}

internal fun clearFlagDetailName(value: Int): String = when (value) {
    1 -> "FAILED"
    2 -> "A-CLEAR"
    3 -> "E-CLEAR"
    4 -> "CLEAR"
    5 -> "H-CLEAR"
    6 -> "EXH-CLEAR"
    7 -> "FULL-COMBO"
    else -> "NO PLAY"
}

internal fun clearFlagColor(value: Int): ComposeColor = when (value) {
    1 -> ClearFailed
    2 -> ClearAssist
    3 -> ClearEasy
    4 -> ClearNormal
    5 -> ClearHard
    6 -> ClearExHard
    7 -> ClearFullCombo
    else -> Muted
}

internal fun rankDeltaText(exScore: Int, noteCount: Int): String =
    rankSummary(exScore, noteCount).substringAfter(' ', "").trim()

internal fun rankDeltaColor(value: String): ComposeColor = when {
    value.trimStart().startsWith("-") -> ClearHard
    value.trimStart().startsWith("+") -> NormalBlue
    else -> Muted
}

internal fun difficultyName(value: String): String = when (value) {
    "B" -> "BEGINNER"
    "N" -> "NORMAL"
    "H" -> "HYPER"
    "A" -> "ANOTHER"
    "L" -> "LEGGENDARIA"
    else -> value
}

internal fun String.optionAbbreviation(): String = when (this) {
    "MIRROR" -> "MIR"
    "RANDOM" -> "RAN"
    else -> "NON"
}
