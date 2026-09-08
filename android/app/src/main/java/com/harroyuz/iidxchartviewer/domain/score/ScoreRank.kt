package com.harroyuz.iidxchartviewer.domain.score

internal fun scoreRankName(exScore: Int, noteCount: Int): String =
    rankSummary(exScore, noteCount).substringBefore(' ').takeIf { it != "—" }.orEmpty()

internal fun listScoreRankName(exScore: Int, noteCount: Int): String {
    val summary = rankSummary(exScore, noteCount)
    val rank = summary.substringBefore(' ').takeIf { it != "—" }.orEmpty()
    val delta = summary.substringAfter(' ', "").trimStart()
    if (!delta.startsWith("-") || rank.isBlank()) return rank
    val rankNames = listOf("F", "E", "D", "C", "B", "A", "AA", "AAA")
    val index = rankNames.indexOf(rank)
    return rankNames.getOrElse((index - 1).coerceAtLeast(0)) { rank }
}

internal fun difficultyIndex(value: String): Int = when (value) {
    "B" -> 0
    "N" -> 1
    "H" -> 2
    "A" -> 3
    "L" -> 4
    else -> -1
}.coerceAtLeast(0)

internal fun rankSummary(exScore: Int, noteCount: Int): String {
    if (noteCount <= 0) return "—"
    val thresholds = (1..8).map { step -> kotlin.math.ceil(noteCount * 2.0 * step / 9.0).toInt() }
    val rankIndex = thresholds.indexOfLast { exScore >= it }
    val rankNames = listOf("F", "E", "D", "C", "B", "A", "AA", "AAA")
    val currentThreshold = thresholds.getOrElse(rankIndex) { 0 }
    val rankPlus = exScore - currentThreshold
    val nextIndex = rankIndex + 1
    val nextThreshold = thresholds.getOrNull(nextIndex)
    if (nextThreshold == null) return "AAA + $rankPlus"
    val nextMinus = nextThreshold - exScore
    return if (rankIndex < 0 || nextMinus < rankPlus) {
        "${rankNames.getOrElse(nextIndex) { "AAA" }} - $nextMinus"
    } else {
        "${rankNames.getOrElse(rankIndex) { "F" }} + $rankPlus"
    }
}
