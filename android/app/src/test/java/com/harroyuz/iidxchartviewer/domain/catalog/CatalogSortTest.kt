package com.harroyuz.iidxchartviewer.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Test
import com.harroyuz.iidxchartviewer.domain.model.IidxChart

class CatalogSortTest {
    @Test fun changingDimensionPreservesDirectionAndChangingDirectionPreservesDimension() {
        assertEquals(CatalogSortOrder.VERSION_DESCENDING, CatalogSortOrder.TITLE_DESCENDING.withDimension(CatalogSortDimension.VERSION))
        assertEquals(CatalogSortOrder.TITLE_ASCENDING, CatalogSortOrder.VERSION_ASCENDING.withDimension(CatalogSortDimension.TITLE))
        for (order in CatalogSortOrder.entries) {
            assertEquals(order.dimension, order.reversed().dimension)
            assertEquals(!order.descending, order.reversed().descending)
            assertEquals(order, order.reversed().reversed())
        }
    }
    private fun sorted(titles: List<String>, order: CatalogSortOrder = CatalogSortOrder.TITLE_ASCENDING) =
        sortCatalogSongs(titles, order, { it }, { it })

    @Test fun defaultOrderIgnoresVersionAndCaseOrCharacterWidth() {
        assertEquals(listOf("alpha", "Ｂｅｔａ", "Zebra"), sorted(listOf("Zebra", "Ｂｅｔａ", "alpha")))
    }

    @Test fun symbolsAndNonLatinTitlesArePreservedAndDigitsUseLexicographicOrder() {
        val titles = listOf("∀", "2 Tribe", "あ", "10 Stars", "!", "A")
        assertEquals(listOf("!", "10 Stars", "2 Tribe", "A", "∀", "あ"), sorted(titles))
        assertEquals(sorted(titles).reversed(), sorted(titles, CatalogSortOrder.TITLE_DESCENDING))
    }

    @Test fun sameTitleVariantsHaveStableOrderingRegardlessOfInputOrder() {
        val songs = listOf("z" to "Song", "b" to "Song", "a" to "song")
        val expected = listOf("b", "z", "a")
        for (input in listOf(songs, songs.reversed())) {
            assertEquals(expected, sortCatalogSongs(input, CatalogSortOrder.TITLE_ASCENDING, { it.second }, { it.first }).map { it.first })
        }
    }

    @Test fun emptyAndSingleSongListsRemainValid() {
        assertEquals(emptyList<String>(), sorted(emptyList()))
        assertEquals(listOf("Song"), sorted(listOf("Song")))
    }

    @Test fun versionSortUsesConsumerThenSubstreamThenNumericVersionAndReversesIndicesToo() {
        data class Song(val title: String, val version: Int, val index: Int)
        val songs = listOf(Song("a", 33, 3301), Song("z", 1, 9), Song("b", 33, 3399),
            Song("cs", -1, 3901), Song("sub", 0, 101), Song("first", 1, 1), Song("second", 2, 201))
        fun order(direction: CatalogSortOrder) = sortCatalogSongs(songs, direction, { it.title }, { it.title }, { it.version }, { it.index }).map { it.title }
        val expected = listOf("cs", "sub", "first", "z", "second", "a", "b")
        assertEquals(expected, order(CatalogSortOrder.VERSION_ASCENDING))
        assertEquals(expected.reversed(), order(CatalogSortOrder.VERSION_DESCENDING))
    }

    @Test fun versionSortingUsesTheOriginalTextageIndexRatherThanTitleOrIncomingOrder() {
        val chart = IidxChart("a", "AAA", mode = "SP", difficulty = "N", level = 1, notes = 1, version = "EPOLIS", textageVersion = 31, textageIndex = 3199)
        val songs = listOf(chart, chart.copy(id = "z", title = "ZZZ", textageIndex = 3101))
        val ranks = buildCatalogVersionOptions(songs).associate { it.value to it.order }
        assertEquals(listOf("ZZZ", "AAA"), sortCatalogSongs(songs, CatalogSortOrder.VERSION_ASCENDING,
            IidxChart::title, IidxChart::id, { ranks.getValue(it.version) }, IidxChart::textageIndex).map { it.title })
    }

    @Test fun missingIndicesDoNotDropOfflineCachedSongs() {
        val titles = listOf("Z", "A")
        assertEquals(listOf("A", "Z"), sorted(titles, CatalogSortOrder.VERSION_ASCENDING))
        assertEquals(listOf("Z", "A"), sorted(titles, CatalogSortOrder.VERSION_DESCENDING))
    }
}
