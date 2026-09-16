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
    val enriched = refreshBjmHistoryReferences(existing, incoming)
    return (enriched + additions)
        .distinctBy(::bjmHistoryRecordKey)
        .sortedWith(compareByDescending<BjmScore> { it.time }.thenByDescending { it.key })
}

internal fun bjmHistoryRecordKey(score: BjmScore): String = "${score.key}:${score.time}"

/** Backfill only an identical achievement, never infer an old chart revision from today's NOTE DB. */
internal fun refreshBjmHistoryReferences(existing: List<BjmScore>, verified: List<BjmScore>): List<BjmScore> {
    val byAchievement = verified.associateBy(::bjmHistoryRecordKey)
    return existing.map { old ->
        if (old.sourceNoteCount != null && old.sourceDjRate != null) return@map old
        val current = byAchievement[bjmHistoryRecordKey(old)] ?: return@map old
        val notes = current.sourceNoteCount ?: return@map old
        if (current.exScore != old.exScore || !current.matchesChartNotes(notes)) return@map old
        if (old.sourceNoteCount != null && old.sourceNoteCount != notes) return@map old
        if (old.sourceDjRate != null && old.sourceDjRate != current.sourceDjRate) return@map old
        old.copy(sourceNoteCount = notes, sourceDjRate = current.sourceDjRate)
    }
}
