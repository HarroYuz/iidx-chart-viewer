package com.harroyuz.iidxchartviewer.domain.catalog

import org.junit.Assert.*
import org.junit.Test

class FavoriteFilterTest {
    @Test fun everyTransitionKeepsAtLeastOneStatusSelected() {
        for (state in FavoriteFilter.entries) {
            for (clicked in listOf(true, false)) {
                val next = state.toggle(clicked)
                assertTrue(next.matches(true) || next.matches(false))
                if (state != FavoriteFilter.ALL && state.matches(clicked)) assertEquals(state, next)
            }
        }
    }

    @Test fun independentPositiveButtonsSupportBothAndEitherStatus() {
        assertEquals(FavoriteFilter.FAVORITED, FavoriteFilter.ALL.toggle(false))
        assertEquals(FavoriteFilter.NOT_FAVORITED, FavoriteFilter.ALL.toggle(true))
        assertEquals(FavoriteFilter.ALL, FavoriteFilter.FAVORITED.toggle(false))
        assertEquals(FavoriteFilter.ALL, FavoriteFilter.NOT_FAVORITED.toggle(true))
        assertTrue(FavoriteFilter.FAVORITED.matches(true))
        assertFalse(FavoriteFilter.FAVORITED.matches(false))
        assertFalse(FavoriteFilter.NOT_FAVORITED.matches(true))
        assertTrue(FavoriteFilter.NOT_FAVORITED.matches(false))
    }
}
