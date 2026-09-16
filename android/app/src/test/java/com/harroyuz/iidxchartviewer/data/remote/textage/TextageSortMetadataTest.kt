package com.harroyuz.iidxchartviewer.data.remote.textage

import com.harroyuz.iidxchartviewer.domain.catalog.buildCatalogVersionOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TextageSortMetadataTest {
    @Test fun capturesIdIndexAndVersionWithoutAssumingObjectOrderOrUsingBjmIds() = runBlocking {
        val source = """
            SS=35;
            titletbl={'late':[1,49,0,'G','C','AAA'],'early':[1,2,0,'G','C','ZZZ'],'sub':[SS,101,0,'G','C','Sub']};
            datatbl={'late':[0,0,100,0,0,0,0,0,0,0,0,'150'],'early':[0,0,100,0,0,0,0,0,0,0,0,'150']};
            actbl={'late':[1,0,0,0,0,1],'early':[1,0,0,0,0,1],'sub':[1,0,0,0,0,1]};
            vertbl=['Consumer only','1st style'];vertbl[35]='substream';
        """.trimIndent()
        val charts = TextageParser.parseCatalog(source) { _, _, _ -> }.associateBy { it.title }
        assertEquals(49, charts.getValue("AAA").textageIndex)
        assertEquals(2, charts.getValue("ZZZ").textageIndex)
        val sub = charts.getValue("Sub")
        assertEquals(35, sub.textageVersion)
        assertEquals(101, sub.textageIndex)
        assertNull(sub.textageUrl)
        assertEquals(0, buildCatalogVersionOptions(listOf(sub)).single().order)
    }
}
