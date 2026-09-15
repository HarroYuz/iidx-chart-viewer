package com.harroyuz.iidxchartviewer.data.remote.textage

import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TextageArcadeStatusTest {
    @Test fun readsArcadeBitBeforeMergingConsumerFlags() = runBlocking {
        val entries = listOf("current", "deleted", "removed", "consumer", "unknown")
        fun levels(flag: String) = "[$flag,0,0,0,0,6,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]"
        val source = """
            B=11;
            titletbl={${entries.joinToString(",") { "'$it':[${if (it == "consumer") 0 else 1},1,1,'GENRE','Artist','$it']" }}};
            datatbl={${entries.joinToString(",") { "'$it':[0,0,500,0,0,0,0,0,0,0,0,'150']" }}};
            actbl={'current':${levels("B")},'deleted':${levels("2")},'removed':${levels("0")},'consumer':${levels("2")}};
            cstbl[8]={'removed':${levels("1")},'unknown':${levels("1")}};
            vertbl=['Consumer only','1st style'];
        """.trimIndent()
        val statuses = TextageParser.parseCatalog(source) { _, _, _ -> }.associate { it.title to it.arcadeStatus }
        assertEquals(ArcadeStatus.CURRENT, statuses["current"])
        assertEquals(ArcadeStatus.DELETED, statuses["deleted"])
        assertEquals(ArcadeStatus.DELETED, statuses["removed"])
        assertEquals(ArcadeStatus.CONSUMER_ONLY, statuses["consumer"])
        assertEquals(ArcadeStatus.UNKNOWN, statuses["unknown"])
    }
}
