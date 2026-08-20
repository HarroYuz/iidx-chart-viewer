package com.harroyuz.iidxchartviewer

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Keeps the WebView login session and native API requests in sync.
 *
 * GTDR uses the same split: WebView owns the persistent browser cookies, while
 * the API client uses a real CookieJar. The jar is rehydrated from WebView
 * cookies before requests and response cookies are written back to WebView,
 * so refresh/session cookies are not lost between the login page and API calls.
 */
class BjmSessionManager private constructor(context: Context) {
    companion object {
        const val ORIGIN = "https://u.bjmania.com"
        const val HOST = "u.bjmania.com"

        private const val DEFAULT_REFERER = "$ORIGIN/"
        private const val XSRF_COOKIE_NAME = "XSRF-TOKEN"
        private const val COOKIE_SYNC_RETRY_COUNT = 8
        private const val COOKIE_SYNC_RETRY_DELAY_MS = 120L

        @Volatile private var instance: BjmSessionManager? = null

        fun getInstance(context: Context): BjmSessionManager =
            instance ?: synchronized(this) {
                instance ?: BjmSessionManager(context.applicationContext).also { instance = it }
            }
    }

    private val webViewCookieManager = CookieManager.getInstance().apply {
        setAcceptCookie(true)
    }
    private val cookieJar = BjmCookieJar()
    private var referer = DEFAULT_REFERER
    private var userAgent = ""

    private val interceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        if (userAgent.isNotBlank() && original.header("User-Agent") == null) {
            builder.header("User-Agent", userAgent)
        }
        if (original.url.host == HOST && original.header("Referer") == null) {
            builder.header("Referer", referer)
        }
        if (original.url.host == HOST && requiresCsrfHeaders(original.method)) {
            if (original.header("X-Requested-With") == null) {
                builder.header("X-Requested-With", "XMLHttpRequest")
            }
            if (original.header("X-XSRF-TOKEN") == null) {
                xsrfToken()?.takeIf(String::isNotBlank)?.let {
                    builder.header("X-XSRF-TOKEN", it)
                }
            }
        }
        val request = builder.build()
        val response = chain.proceed(request)
        if (request.url.host == HOST) persistResponseCookies(request.url, response)
        response
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor(interceptor)
        .build()

    /** A probe must use the cookies currently held by WebView, even before a request sync. */
    private val probeClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor(interceptor)
        .build()

    fun client(): OkHttpClient = client

    @Synchronized
    fun setUserAgent(value: String) {
        if (value.isNotBlank()) userAgent = value
    }

    @Synchronized
    fun setReferer(value: String) {
        val uri = Uri.parse(value.trim())
        val normalized = value.trim().takeIf { uri.getScheme() == "https" && uri.getHost() == HOST }
        if (!normalized.isNullOrBlank()) referer = normalized
    }

    @Synchronized
    fun syncFromWebViewCookieManager() {
        val header = readWebViewCookieHeaderWithWarmup() ?: return
        cookieJar.replaceAll(parseWebViewCookies(header))
    }

    @Synchronized
    fun hasWebViewCookies(): Boolean = !webViewCookieManager.getCookie(ORIGIN).isNullOrBlank()

    @Synchronized
    fun clearNativeSession() {
        cookieJar.clear()
    }

    @Synchronized
    fun clearAllSession() {
        clearNativeSession()
        val current = webViewCookieManager.getCookie(ORIGIN).orEmpty()
        current.split(';')
            .asSequence()
            .map(String::trim)
            .mapNotNull { item -> item.indexOf('=').takeIf { it > 0 }?.let { item.substring(0, it).trim() } }
            .distinct()
            .forEach { name ->
                webViewCookieManager.setCookie(
                    ORIGIN,
                    "$name=; Expires=Wed, 31 Dec 2000 23:59:59 GMT; Path=/",
                )
            }
        webViewCookieManager.flush()
    }

    fun probeAuthMeWithWebViewCookies(): AuthCheckResult {
        val cookieHeader = readWebViewCookieHeaderWithWarmup()
        if (cookieHeader.isNullOrBlank()) return AuthCheckResult(false, 0, false)
        val request = Request.Builder()
            .url("$ORIGIN/api/auth/me")
            .header("Accept", "application/json")
            .header("Cookie", cookieHeader)
            .get()
            .build()
        probeClient.newCall(request).execute().use { response ->
            webViewCookieManager.flush()
            syncFromWebViewCookieManager()
            return AuthCheckResult(response.isSuccessful, response.code, cookieHeader.length > 0)
        }
    }

    private fun requiresCsrfHeaders(method: String): Boolean =
        !method.equals("GET", ignoreCase = true) && !method.equals("HEAD", ignoreCase = true)

    @Synchronized
    private fun xsrfToken(): String? {
        val header = readWebViewCookieHeaderWithWarmup() ?: return null
        return extractCookieValue(header, XSRF_COOKIE_NAME)
            ?.let { runCatching { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }.getOrNull() }
    }

    private fun persistResponseCookies(url: HttpUrl, response: Response) {
        response.headers("Set-Cookie").forEach { cookie ->
            webViewCookieManager.setCookie(url.toString(), cookie)
        }
        if (response.headers("Set-Cookie").isNotEmpty()) webViewCookieManager.flush()
    }

    private fun parseWebViewCookies(header: String): List<Cookie> =
        header.split(';')
            .asSequence()
            .map(String::trim)
            .mapNotNull { item ->
                val separator = item.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val name = item.substring(0, separator).trim()
                val value = item.substring(separator + 1).trim()
                if (name.isBlank()) null else Cookie.Builder()
                    .name(name)
                    .value(value)
                    .hostOnlyDomain(HOST)
                    .path("/")
                    .secure()
                    .build()
            }
            .toList()

    private fun readWebViewCookieHeaderWithWarmup(): String? {
        repeat(COOKIE_SYNC_RETRY_COUNT + 1) { attempt ->
            webViewCookieManager.getCookie(ORIGIN)?.takeIf(String::isNotBlank)?.let { return it }
            if (attempt < COOKIE_SYNC_RETRY_COUNT) SystemClock.sleep(COOKIE_SYNC_RETRY_DELAY_MS)
        }
        return null
    }

    private fun extractCookieValue(header: String, name: String): String? =
        header.split(';')
            .asSequence()
            .map(String::trim)
            .mapNotNull { item ->
                val separator = item.indexOf('=')
                if (separator > 0 && item.substring(0, separator).trim() == name) {
                    item.substring(separator + 1).trim()
                } else null
            }
            .firstOrNull()

    data class AuthCheckResult(val success: Boolean, val statusCode: Int, val hadCookie: Boolean)
}

private class BjmCookieJar : CookieJar {
    private val cookies = linkedMapOf<String, Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        cookies.forEach { cookie ->
            if (cookie.expiresAt < now) this.cookies.remove(key(cookie)) else this.cookies[key(cookie)] = cookie
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val iterator = cookies.iterator()
        val result = mutableListOf<Cookie>()
        while (iterator.hasNext()) {
            val cookie = iterator.next().value
            if (cookie.expiresAt < now) iterator.remove()
            else if (cookie.matches(url)) result += cookie
        }
        return result
    }

    @Synchronized
    fun replaceAll(values: List<Cookie>) {
        cookies.clear()
        val now = System.currentTimeMillis()
        values.filter { it.expiresAt >= now }.forEach { cookies[key(it)] = it }
    }

    @Synchronized
    fun clear() = cookies.clear()

    private fun key(cookie: Cookie): String = "${cookie.name}@${cookie.domain}${cookie.path}"
}
