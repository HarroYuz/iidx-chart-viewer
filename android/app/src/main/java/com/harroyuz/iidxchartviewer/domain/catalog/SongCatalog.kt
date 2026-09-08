package com.harroyuz.iidxchartviewer.domain.catalog

import com.harroyuz.iidxchartviewer.domain.model.BjmMusic
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.model.IidxSongGroup
import java.text.Normalizer
import java.util.Locale

private val MusicTitleFilterRegex = Regex("[^\\p{L}\\p{N}]")

internal fun versionNumber(value: String): Int =
    Regex("\\d+").find(value)?.value?.toIntOrNull() ?: Int.MAX_VALUE

internal fun textageVersionIndex(url: String): Int? =
    Regex("/score/([^/]+)/").find(url)?.groupValues?.getOrNull(1)?.let { directory ->
        if (directory == "s") 35 else directory.toIntOrNull()
    }

internal fun chartSongKey(chart: IidxChart): String =
    chart.textageUrl
        ?.substringBefore('?')
        ?.substringAfterLast('/')
        ?.substringBeforeLast('.')
        ?.takeIf { it.isNotBlank() }
        ?: chart.id.substringBeforeLast('-').removePrefix("textage-")

internal fun songGroupKey(chart: IidxChart): String = listOf(
    chart.title,
    chart.subtitle,
    chart.genre,
    chart.composer,
    chart.sourceLabel,
).joinToString("\u0000", transform = ::normalizeMusicTitle)

internal fun difficultyOrder(value: String): Int = when (value) {
    "B" -> 0
    "N" -> 1
    "H" -> 2
    "A" -> 3
    "L" -> 4
    else -> 9
}

private fun normalizeMusicTitle(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(MusicTitleFilterRegex, "")

internal fun buildSongGroups(charts: List<IidxChart>): List<IidxSongGroup> =
    charts.groupBy(::songGroupKey).map { (key, group) ->
        val first = group.first()
        IidxSongGroup(
            key = key,
            title = first.title,
            subtitle = first.subtitle,
            genre = first.genre,
            composer = first.composer,
            version = first.version,
            sourceLabel = first.sourceLabel,
            chartIds = group.map(IidxChart::id),
        )
    }

internal fun buildBjmMusicIndex(music: List<BjmMusic>): Map<String, List<BjmMusic>> =
    music
        .flatMap { candidate ->
            setOf(candidate.title, candidate.plainTitle)
                .map(::normalizeMusicTitle)
                .filter(String::isNotBlank)
                .map { it to candidate }
        }
        .groupBy({ it.first }, { it.second })

internal fun findBjmMusic(chart: IidxChart, index: Map<String, List<BjmMusic>>): BjmMusic? {
    val titleKeys = buildList {
        if (chart.subtitle.isNotBlank()) add(normalizeMusicTitle("${chart.title} ${chart.subtitle}"))
        add(normalizeMusicTitle(chart.title))
    }.filter(String::isNotBlank).distinct()
    val candidates = titleKeys
        .flatMap { index[it].orEmpty() }
        .distinctBy { it.musicId }
    if (candidates.isEmpty()) return null
    val chartVersion = chart.textageUrl?.let(::textageVersionIndex)
    return candidates.maxWithOrNull(
        compareBy<BjmMusic>(
            { candidate ->
                val candidateKeys = setOf(
                    normalizeMusicTitle(candidate.title),
                    normalizeMusicTitle(candidate.plainTitle),
                )
                titleKeys.indexOfFirst(candidateKeys::contains)
                    .takeIf { it >= 0 }
                    ?.let { titleKeys.size - it }
                    ?: 0
            },
            { it.level(chart.mode, chart.difficulty) == chart.level },
            { chartVersion != null && it.version == chartVersion },
            { it.version },
        ),
    )
}
