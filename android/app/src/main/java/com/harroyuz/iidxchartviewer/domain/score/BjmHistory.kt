package com.harroyuz.iidxchartviewer.domain.score

import com.harroyuz.iidxchartviewer.domain.model.BjmScore

/**
 * Merges the records returned by BJM into the independent local history.
 *
 * BJM returns the current best record for each chart. The record's time is
 * used as the update timestamp, so a later best-record update can be kept even
 * after the previous best record disappears from the current-score database.
 */
internal fun appendBjmHistory(
    existing: List<BjmScore>,
    incoming: List<BjmScore>,
): List<BjmScore> {
    val latestTime = existing.maxOfOrNull { it.time } ?: Long.MIN_VALUE
    val known = existing.mapTo(mutableSetOf(), ::bjmHistoryRecordKey)
    val additions = incoming.filter { score ->
        score.time > latestTime && known.add(bjmHistoryRecordKey(score))
    }
    val refreshed = incoming.associateBy(::bjmHistoryRecordKey)
    val enriched = existing.map { old ->
        val current = refreshed[bjmHistoryRecordKey(old)]
        if (current != null && current.exScore == old.exScore &&
            current.sourceNoteCount != null && current.sourceDjRate != null
        ) old.copy(sourceNoteCount = current.sourceNoteCount, sourceDjRate = current.sourceDjRate)
        else old
    }
    return (enriched + additions)
        .distinctBy(::bjmHistoryRecordKey)
        .sortedWith(compareByDescending<BjmScore> { it.time }.thenByDescending { it.key })
}

internal fun bjmHistoryRecordKey(score: BjmScore): String = "${score.key}:${score.time}"
