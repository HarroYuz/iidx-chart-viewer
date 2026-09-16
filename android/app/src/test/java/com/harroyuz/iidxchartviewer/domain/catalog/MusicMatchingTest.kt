package com.harroyuz.iidxchartviewer.domain.catalog

import com.harroyuz.iidxchartviewer.domain.model.BjmMusic
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicMatchingTest {
    private fun chart(title: String, subtitle: String = "") = IidxChart(
        id = "textage-test-spa", title = title, subtitle = subtitle,
        mode = "SP", difficulty = "A", level = 11, notes = 1000, version = "Rootage",
        textageUrl = "https://textage.cc/score/26/test.html",
    )

    private fun match(title: String, music: List<BjmMusic>, subtitle: String = ""): Int? =
        findBjmMusic(chart(title, subtitle), buildBjmMusicIndex(music))?.musicId

    @Test fun pureSymbolTitlesMatchWithoutCollidingWithOtherSymbolsOrBlankTitles() {
        val music = listOf(BjmMusic(28005, "∀", "TURN A"), BjmMusic(30029, "≡+≡", "3+3"), BjmMusic(1, ""))
        assertEquals(28005, match("∀", music))
        assertEquals(30029, match("≡＋≡", music))
        assertNull(match("★", music))
        assertNull(match(" ", music))
    }

    @Test fun exactNormalizationSupportsWidthCaseAndPlainTitle() {
        assertEquals(1, match("  Ａ＋Ｂ  ", listOf(BjmMusic(1, "a+b"))))
        assertEquals(2, match("Turn A", listOf(BjmMusic(2, "∀", "TURN A"))))
    }

    @Test fun exactMatchWinsOverLooseCandidateEvenWhenLooseHasBetterLevelAndVersion() {
        val music = listOf(
            BjmMusic(1, "A+B", version = 25),
            BjmMusic(2, "AB", version = 26, levels = listOf("0", "0", "0", "11")),
        )
        assertEquals(1, match("A+B", music))
        assertEquals(2, match("AB", music))
    }

    @Test fun exactFullTitleHasPriorityOverExactBareTitle() {
        val music = listOf(BjmMusic(1, "Song"), BjmMusic(2, "Song (Mix)"))
        assertEquals(2, match("Song", music, "(Mix)"))
    }

    @Test fun looseFullTitleWinsOverExactBareTitleToPreserveMixIdentity() {
        val music = listOf(BjmMusic(1, "Song"), BjmMusic(2, "Song Mix"))
        assertEquals(2, match("Song", music, "(Mix)"))
    }

    @Test fun knownRemixesKeepTheirIdentityWhenBaseSongsAlsoExist() {
        assertEquals(16002, match("B4U", listOf(BjmMusic(4002, "B4U"),
            BjmMusic(16002, "B4U(BEMANI FOR YOU MIX)")), "(BEMANI FOR YOU MIX)"))
        assertEquals(1303, match("Clione", listOf(BjmMusic(4005, "Clione"),
            BjmMusic(1303, "Clione (Ryu* Remix)")), "(Ryu☆ Remix)"))
    }

    @Test fun legacyFallbackStillHandlesPunctuationAndSubtitleDifferences() {
        assertEquals(1, match("LOVE♥SHINE", listOf(BjmMusic(1, "LOVE♡SHINE"))))
        assertEquals(2, match("Song", listOf(BjmMusic(1, "S-o-n-g"), BjmMusic(2, "Song Mix")), "(Mix)"))
    }

    @Test fun confirmedAliasesResolveToTheApprovedIds() {
        val aliases = listOf(
            Triple("CODE:Ø", "CODE:0", 26016),
            Triple("FiZZλ_PØT!OИ", "FiZZλ_PØT!0И", 33018),
            Triple("POLꓘAMAИIA", "POLꞰAMAИIA", 28050),
            Triple("uәn", "uən", 32006),
            Triple("Χ-DEN", "X-DEN", 26007),
        )
        val music = aliases.map { (_, target, id) -> BjmMusic(id, target) }
        aliases.forEach { (source, _, id) -> assertEquals(source, id, match(source, music)) }
    }

    @Test fun aliasesAreWholeTitleSpecificAndRequireTheTargetInTheCatalog() {
        assertNull(match("CODE:Ø", listOf(BjmMusic(99, "CODE:0"))))
        assertNull(match("OTHER:Ø", listOf(BjmMusic(26016, "CODE:0"), BjmMusic(1, "OTHER:0"))))
        assertNull(match("Χ-OTHER", listOf(BjmMusic(1, "X-OTHER"))))
    }

    @Test fun exactTitleWinsOverAliasAndAliasWinsOverLooseCollision() {
        val alias = BjmMusic(26016, "CODE:0")
        assertEquals(1, match("CODE:Ø", listOf(alias, BjmMusic(1, "CODE:Ø"))))
        assertEquals(26016, match("CODE:Ø", listOf(alias, BjmMusic(2, "CODEØ"))))
    }

    @Test fun sameTitleCandidatesKeepExistingLevelThenVersionPreference() {
        val music = listOf(
            BjmMusic(1, "Song", version = 30, levels = listOf("0", "0", "0", "11")),
            BjmMusic(2, "Song", version = 26, levels = listOf("0", "0", "0", "11")),
            BjmMusic(3, "Song", version = 31),
        )
        assertEquals(2, match("Song", music))
    }
}
