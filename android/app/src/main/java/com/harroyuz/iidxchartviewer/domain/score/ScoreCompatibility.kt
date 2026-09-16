package com.harroyuz.iidxchartviewer.domain.score

import com.harroyuz.iidxchartviewer.domain.catalog.preferredChartOrder
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.model.BjmScore

/** Actual DJ RATE, not the nearest target rank used by the score delta UI. */
internal fun djRate(exScore: Int, noteCount: Int): String? {
    if (noteCount <= 0 || exScore < 0 || exScore.toLong() > noteCount.toLong() * 2) return null
    val step = (exScore.toLong() * 9 / (noteCount.toLong() * 2)).toInt()
    return listOf("F", "E", "D", "C", "B", "A", "AA", "AAA")[(step - 1).coerceIn(0, 7)]
}

/** BJM's score RPC has no DJ RATE: its site derives it from its separate NOTE database.
 * Keep that reference with the score; deriving both grades from Textage would prove nothing.
 * Equal grades alone cannot distinguish chart revisions, so NOTE counts must also agree.
 */
internal fun BjmScore.matchesChartNotes(noteCount: Int): Boolean =
    sourceNoteCount != null && sourceNoteCount == noteCount &&
        sourceDjRate != null && sourceDjRate == djRate(exScore, noteCount)

internal fun BjmScore.withNoteReference(noteCount: Int?): BjmScore = copy(
    sourceNoteCount = noteCount?.takeIf { it > 0 },
    sourceDjRate = noteCount?.let { djRate(exScore, it) },
)

internal fun preferredChartForScore(
    score: BjmScore,
    candidates: List<IidxChart>,
): IidxChart? = candidates
    .filter { score.matchesChartNotes(it.notes) }
    .maxWithOrNull(preferredChartOrder)

internal data class BjmHistoryTarget(val navigationChart: IidxChart?, val compatibleChart: IidxChart?)

/** Song navigation only requires a known music/style/difficulty; score attachment still requires NOTE evidence. */
internal fun resolveBjmHistoryTarget(score: BjmScore, candidates: List<IidxChart>): BjmHistoryTarget {
    if (score.playStyle !in 0..1 || score.noteId !in 0..4) return BjmHistoryTarget(null, null)
    val sameDifficulty = candidates.filter {
        it.mode == (if (score.playStyle == 1) "DP" else "SP") && difficultyIndex(it.difficulty) == score.noteId
    }
    val compatible = preferredChartForScore(score, sameDifficulty)
    return BjmHistoryTarget(compatible ?: sameDifficulty.maxWithOrNull(preferredChartOrder), compatible)
}
