package com.harroyuz.iidxchartviewer.app

import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import org.junit.Assert.*
import org.junit.Test

class BrowseSelectionTest {
    private val sp = IidxChart("sp", "Song", mode = "SP", difficulty = "A", level = 12, notes = 1000, version = "33")
    private val dp = sp.copy(id = "dp", mode = "DP", notes = 1200)

    @Test fun switchingStyleThenReturningAndReopeningKeepsTheNewStyle() {
        for ((initial, next) in listOf(sp to dp, dp to sp)) {
            val playing = BrowseSelection(song = initial).openChart(initial).openChart(next)
            val detail = playing.back()
            assertNull(detail.chart)
            assertEquals(next, detail.song)
            val reopened = detail.openChart(requireNotNull(detail.song))
            assertEquals(next.mode, reopened.chart?.mode)
            assertEquals(next.id, reopened.chart?.id)
        }
    }

    @Test fun quickBrowsingStillReturnsDirectlyToTheListAfterSwitchingStyle() {
        val playing = BrowseSelection().openChart(sp).openChart(dp)
        assertNull(playing.song)
        assertEquals(BrowseSelection(), playing.back())
    }

    @Test fun changingDifficultyAndSwitchingStyleRepeatedlyKeepsReturnSelectionCurrent() {
        val hyper = dp.copy(id = "dph", difficulty = "H")
        val detail = BrowseSelection(song = sp).openChart(dp).openChart(hyper).back()
        assertEquals(hyper, detail.song)
        assertEquals(sp, detail.openChart(sp).back().song)
        assertEquals(BrowseSelection(), detail.back())
    }
}
