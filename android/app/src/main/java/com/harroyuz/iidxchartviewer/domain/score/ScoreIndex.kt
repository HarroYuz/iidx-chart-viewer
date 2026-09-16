package com.harroyuz.iidxchartviewer.domain.score

import com.harroyuz.iidxchartviewer.domain.catalog.buildBjmMusicIndex
import com.harroyuz.iidxchartviewer.domain.catalog.difficultyOrder
import com.harroyuz.iidxchartviewer.domain.catalog.findBjmMusic
import com.harroyuz.iidxchartviewer.domain.model.BjmIndex
import com.harroyuz.iidxchartviewer.domain.model.IidxAppState
import com.harroyuz.iidxchartviewer.domain.model.IidxChart

private fun buildSongMusicIds(state: IidxAppState): Map<String, Int> {
    val musicIndex = buildBjmMusicIndex(state.bjmMusic)
    val chartsById = state.charts.associateBy { it.id }
    return state.songGroups.asSequence().mapNotNull { songGroup ->
            val songCharts = songGroup.chartIds.mapNotNull { chartId -> chartsById[chartId] }
            val representative = songCharts.minWithOrNull(
                compareBy<IidxChart>({ it.textageUrl == null }, { -it.arcadeStatus.priority }, { it.level <= 0 }, { difficultyOrder(it.difficulty) }),
            ) ?: return@mapNotNull null
            findBjmMusic(representative, musicIndex)?.musicId?.let { musicId -> songGroup.key to musicId }
        }
        .toMap()
}

internal fun buildBjmIndex(
    state: IidxAppState,
    textageRevision: Long,
    musicRevision: Long,
    scoresRevision: Long,
): BjmIndex {
    return BjmIndex(
        songMusicIds = buildSongMusicIds(state),
        scoresByKey = state.bjmScores.associateBy { it.key },
        textageRevision = textageRevision,
        musicRevision = musicRevision,
        scoresRevision = scoresRevision,
        built = true,
    )
}

internal fun rebuildBjmCatalogIndex(
    state: IidxAppState,
    previous: BjmIndex,
    textageRevision: Long,
    musicRevision: Long,
    scoresRevision: Long,
): BjmIndex = previous.copy(
    songMusicIds = buildSongMusicIds(state),
    textageRevision = textageRevision,
    musicRevision = musicRevision,
    scoresRevision = scoresRevision,
    built = true,
)

internal fun rebuildBjmScoresIndex(
    state: IidxAppState,
    previous: BjmIndex,
    textageRevision: Long,
    musicRevision: Long,
    scoresRevision: Long,
): BjmIndex = previous.copy(
    scoresByKey = state.bjmScores.associateBy { it.key },
    textageRevision = textageRevision,
    musicRevision = musicRevision,
    scoresRevision = scoresRevision,
    built = true,
)

internal fun isPersistedBjmIndexUsable(
    state: IidxAppState,
    index: BjmIndex,
    textageRevision: Long,
    musicRevision: Long,
    scoresRevision: Long,
): Boolean {
    if (!index.built) return false
    if (index.textageRevision != textageRevision) return false
    if (index.musicRevision != musicRevision) return false
    if (index.scoresRevision != scoresRevision) return false
    if (index.songMusicIds.any { it.key.isBlank() || it.value <= 0 }) return false
    if (index.scoresByKey.any { (key, score) -> key.isBlank() || key != score.key }) return false
    if (state.bjmScores.isNotEmpty() && index.scoresByKey.isEmpty()) return false
    return true
}

/** Reverse the same song/music links used by the catalog, including confirmed title aliases. */
internal fun buildBjmHistoryChartIndex(state: IidxAppState, bjmIndex: BjmIndex): Map<String, List<IidxChart>> {
    val chartsById = state.charts.associateBy { it.id }
    return buildMap {
        state.songGroups.forEach { group ->
            val musicId = bjmIndex.songMusicIds[group.key] ?: return@forEach
            group.chartIds.mapNotNull { chartsById[it] }
                .groupBy { chart -> "$musicId:${if (chart.mode == "DP") 1 else 0}:${difficultyIndex(chart.difficulty)}" }
                .forEach { (key, candidates) -> put(key, (get(key).orEmpty() + candidates).distinctBy { it.id }) }
        }
    }
}
