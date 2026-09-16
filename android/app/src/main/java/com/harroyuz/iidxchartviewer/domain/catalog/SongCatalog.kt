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

private fun normalizeExactMusicTitle(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).lowercase(Locale.ROOT)

private fun normalizeMusicTitle(value: String): String =
    normalizeExactMusicTitle(value).replace(MusicTitleFilterRegex, "")

// Confirmed catalog aliases, not general substitutions of visually similar characters.
private val confirmedMusicAliases = mapOf(
    "CODE:Ø" to 26016,
    "FiZZλ_PØT!OИ" to 33018,
    "POLꓘAMAИIA" to 28050,
    "uәn" to 32006,
    "Χ-DEN" to 26007,
).mapKeys { normalizeExactMusicTitle(it.key) }

internal data class BjmMusicLookup(
    val exactTitles: Map<String, List<BjmMusic>>,
    val looseTitles: Map<String, List<BjmMusic>>,
    val byId: Map<Int, BjmMusic>,
)

internal val preferredChartOrder: Comparator<IidxChart> = compareBy<IidxChart>(
    { it.textageUrl != null }, { it.arcadeStatus.priority }, { it.notes }, { it.bpm.isNotBlank() },
)

internal fun buildSongGroups(charts: List<IidxChart>): List<IidxSongGroup> =
    charts.groupBy(::songGroupKey).map { (key, group) ->
        val first = group.maxWithOrNull(preferredChartOrder) ?: group.first()
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

internal fun buildBjmMusicIndex(music: List<BjmMusic>): BjmMusicLookup {
    fun index(normalize: (String) -> String): Map<String, List<BjmMusic>> = music
        .flatMap { candidate ->
            setOf(candidate.title, candidate.plainTitle)
                .map(normalize)
                .filter(String::isNotBlank)
                .distinct()
                .map { it to candidate }
        }
        .groupBy({ it.first }, { it.second })
    return BjmMusicLookup(
        exactTitles = index(::normalizeExactMusicTitle),
        looseTitles = index(::normalizeMusicTitle),
        byId = music.associateBy { it.musicId },
    )
}

internal fun findBjmMusic(chart: IidxChart, index: BjmMusicLookup): BjmMusic? {
    val titles = buildList {
        if (chart.subtitle.isNotBlank()) add("${chart.title} ${chart.subtitle}")
        add(chart.title)
    }
    fun match(candidates: List<BjmMusic>): BjmMusic? {
        val chartVersion = chart.textageUrl?.let(::textageVersionIndex)
        return candidates.distinctBy { it.musicId }.maxWithOrNull(
            compareBy<BjmMusic>(
                { it.level(chart.mode, chart.difficulty) == chart.level },
                { chartVersion != null && it.version == chartVersion },
                { it.version },
            ),
        )
    }
    // Preserve the full title's priority so a remix cannot be displaced by its base song.
    // Within each title, prefer exact symbols, then confirmed aliases, then the legacy key.
    titles.forEach { title ->
        val exactKey = normalizeExactMusicTitle(title)
        match(index.exactTitles[exactKey].orEmpty())?.let { return it }
        confirmedMusicAliases[exactKey]?.let { id ->
            index.byId[id]?.let { return it }
        }
        val looseKey = normalizeMusicTitle(title)
        match(index.looseTitles[looseKey].orEmpty())?.let { return it }
    }
    return null
}
