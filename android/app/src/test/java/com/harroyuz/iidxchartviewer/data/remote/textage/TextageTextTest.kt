package com.harroyuz.iidxchartviewer.data.remote.textage

import com.harroyuz.iidxchartviewer.data.text.decodeHtmlEntities
import com.harroyuz.iidxchartviewer.data.local.decodeLegacyTextageEntities
import com.harroyuz.iidxchartviewer.data.local.decodeLegacyBjmEntities
import com.harroyuz.iidxchartviewer.data.remote.bjm.BjmMusicProto
import com.harroyuz.iidxchartviewer.domain.catalog.buildBjmMusicIndex
import com.harroyuz.iidxchartviewer.domain.catalog.buildSongGroups
import com.harroyuz.iidxchartviewer.domain.catalog.findBjmMusic
import com.harroyuz.iidxchartviewer.domain.model.BjmMusic
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TextageTextTest {
    @Test fun decodesNamedEntitiesFoundInThePublicTextageCatalog() {
        assertEquals("Amor De Verão", decodeHtmlEntities("Amor De Ver&atilde;o"))
        assertEquals("♥ ä É Ø é ö Ü ø ê Ë Æ ¡ æ ²", decodeHtmlEntities(
            "&hearts; &auml; &Eacute; &Oslash; &eacute; &ouml; &Uuml; &oslash; &ecirc; &Euml; &AElig; &iexcl; &aelig; &sup2;",
        ))
    }

    @Test fun supportsDecimalHexAndSupplementaryUnicodeWithoutTruncation() {
        assertEquals("í 🟢 🟢", decodeHtmlEntities("&#0237; &#x1F7E2; &#128994;"))
        assertEquals("�", decodeHtmlEntities("&#x110000;"))
    }

    @Test fun decodesOnlyOnceAndPreservesLiteralMarkupAndUnknownEntities() {
        assertEquals("&lt;", decodeHtmlEntities("&amp;lt;"))
        assertEquals("<title> &madeupEntity;", decodeHtmlEntities("&lt;title&gt; &madeupEntity;"))
        assertEquals("A & B", decodeHtmlEntities("A&nbsp;&amp;&nbsp;B"))
        assertEquals("É é", decodeHtmlEntities("&Eacute; &eacute;"))
    }

    @Test fun parsesAndMatchesEncodedTitleUsingTheProductionMatcher() = runBlocking {
        val source = """
            titletbl={'amrverao':[25,2506,0,"熱帯 THE BASS","かめりあ feat. ななひら","Amor De Ver&atilde;o"]};
            datatbl={'amrverao':[0,0,400,900,1200,0,0,500,1000,1300,0,"180"]};
            actbl={'amrverao':[1,0,0,0,0,5,0,9,0,11,0,0,0,0,0,5,0,9,0,11,0,0,0]};
        """.trimIndent()
        val charts = TextageParser.parseCatalog(source) { _, _, _ -> }
        assertTrue(charts.isNotEmpty())
        assertTrue(charts.all { it.title == "Amor De Verão" })
        val index = buildBjmMusicIndex(listOf(BjmMusic(25033, "Amor De Verão", "Amor De Verao")))
        assertTrue(charts.all { findBjmMusic(it, index)?.musicId == 25033 })
    }

    @Test fun migratesCachedTextWithoutChangingChartIdentityScoresOrPlaybackData() {
        val legacy = IidxChart(
            "textage-amrverao-spa", "Amor De Ver&atilde;o", composer = "&Uuml;ber", mode = "SP",
            difficulty = "A", level = 11, notes = 1200, version = "CANNON BALLERS",
            textageUrl = "https://textage.cc/score/25/amrverao.html", score = 2000, confirmed = true,
        )
        val migrated = legacy.decodeLegacyTextageEntities()
        assertEquals(legacy.copy(title = "Amor De Verão", composer = "Über"), migrated)
        assertEquals(listOf(legacy.id), buildSongGroups(listOf(migrated)).single().chartIds)
        assertEquals(25033, findBjmMusic(migrated, buildBjmMusicIndex(listOf(BjmMusic(25033, "Amor De Verão"))))?.musicId)
    }

    @Test fun bjmEncodedTitleAndPlainTitleDecodeBeforeMatchingAndAlsoMigrateOffline() {
        val raw = "Raison d'&ecirc;tre～交差する宿命～"
        val title = raw.toByteArray()
        val musicMessage = byteArrayOf(8, 1, 18, title.size.toByte()) + title
        val encoded = byteArrayOf(10, musicMessage.size.toByte()) + musicMessage
        val music = BjmMusicProto.decode(encoded).single()
        assertEquals("Raison d'être～交差する宿命～", music.title)
        assertEquals(music, BjmMusic(1, raw, levels = List(10) { "" }).decodeLegacyBjmEntities())
        val chart = IidxChart("raison", "Raison d'être", subtitle = "～交差する宿命～", mode = "SP", difficulty = "A", level = 11, notes = 1000, version = "17")
        assertEquals(1, findBjmMusic(chart, buildBjmMusicIndex(listOf(music)))?.musicId)
    }

    @Test fun specialCharactersStillDoNotTriggerFuzzyOrAmbiguousTitleMatching() {
        val chart = IidxChart("one", "∀", mode = "SP", difficulty = "A", level = 12, notes = 1000, version = "28")
        assertNull(findBjmMusic(chart, buildBjmMusicIndex(listOf(BjmMusic(1, "A")))))
    }
}
