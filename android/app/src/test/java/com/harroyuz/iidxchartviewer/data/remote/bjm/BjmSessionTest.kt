package com.harroyuz.iidxchartviewer.data.remote.bjm

import java.io.IOException
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class BjmSessionTest {
    @Test fun authErrorsRecoverAndRetryOnlyOnce() {
        for (first in listOf(401, 403)) for (second in listOf(200, 401, 403)) {
            var requests = 0
            var refreshes = 0
            val result = withBjmAuthRecovery(
                { if (++requests == 1) first else second }, { it }, { refreshes++ },
            )
            assertEquals(second, result)
            assertEquals(2, requests)
            assertEquals(1, refreshes)
        }
    }

    @Test fun ordinaryResponsesDoNotRefresh() {
        for (code in listOf(200, 302, 400, 404, 429, 500, 502, 503)) {
            var requests = 0
            assertEquals(code, withBjmAuthRecovery({ requests++; code }, { it }, { fail("Unexpected refresh") }))
            assertEquals(1, requests)
        }
    }

    @Test fun networkFailuresBeforeOrAfterRecoveryPropagateWithoutExtraRetries() {
        for (failOn in listOf(1, 2)) {
            var requests = 0
            var refreshes = 0
            assertThrows(IOException::class.java) {
                withBjmAuthRecovery<Int>({ if (++requests == failOn) throw IOException("offline") else 401 }, { it }, { refreshes++ })
            }
            assertEquals(failOn, requests)
            assertEquals(failOn - 1, refreshes)
        }
    }

    @Test fun cookieJarKeepsTokensScopesAndServerRotation() {
        val jar = BjmCookieJar()
        val url = "https://u.bjmania.com/api/auth/me".toHttpUrl()
        val cookie = Cookie.Builder().name("session").value("first").hostOnlyDomain(url.host).path("/api").secure().build()
        jar.saveFromResponse(url, listOf(cookie))
        assertEquals("first", jar.loadForRequest(url).single().value)
        assertTrue(jar.loadForRequest("https://other.example/api/auth/me".toHttpUrl()).isEmpty())
        assertTrue(jar.loadForRequest("https://u.bjmania.com/elsewhere".toHttpUrl()).isEmpty())
        jar.saveFromResponse(url, listOf(Cookie.Builder().name("session").value("rotated").hostOnlyDomain(url.host).path("/api").secure().build()))
        assertEquals("rotated", jar.loadForRequest(url).single().value)
        jar.clear()
        assertTrue(jar.loadForRequest(url).isEmpty())
    }
}
