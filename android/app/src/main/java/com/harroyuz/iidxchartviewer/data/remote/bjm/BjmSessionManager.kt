package com.harroyuz.iidxchartviewer.data.remote.bjm

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Synchronizes persistent WebView credentials with the native API cookie jar. */
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
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val webViewRefreshLock = Any()
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
        if (cookieHeader.isNullOrBlank()) return AuthCheckResult(false, 0, 0, false, "")
        val request = Request.Builder()
            .url("$ORIGIN/api/auth/me")
            .header("Accept", "application/json")
            .header("Cookie", cookieHeader)
            .get()
            .build()
        probeClient.newCall(request).execute().use { response ->
            webViewCookieManager.flush()
            syncFromWebViewCookieManager()
            return AuthCheckResult(
                success = response.isSuccessful,
                statusCode = response.code,
                cookieLength = cookieHeader.length,
                hadCookie = true,
                body = response.body?.string().orEmpty(),
            )
        }
    }

    /**
     * Gives WebView one silent request to receive any refreshed session
     * cookies. This is intentionally blocking only the calling worker thread;
     * WebView creation and callbacks remain on the main thread.
     */
    fun refreshWebViewCookiesBlocking(timeoutMillis: Long = 8_000L): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) return false
        synchronized(webViewRefreshLock) {
            val latch = CountDownLatch(1)
            val refreshed = AtomicBoolean(false)
            val finished = AtomicBoolean(false)
            val webViewRef = AtomicReference<WebView?>(null)
            val currentUserAgent = synchronized(this) { userAgent }

            fun finish(success: Boolean) {
                if (!finished.compareAndSet(false, true)) return
                if (success) webViewCookieManager.flush()
                webViewRef.getAndSet(null)?.let { webView ->
                    webView.stopLoading()
                    webView.destroy()
                }
                refreshed.set(success)
                latch.countDown()
            }

            mainHandler.post {
                if (finished.get()) return@post
                try {
                    val webView = WebView(appContext)
                    webViewRef.set(webView)
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    if (currentUserAgent.isNotBlank()) {
                        webView.settings.userAgentString = currentUserAgent
                    }
                    webViewCookieManager.setAcceptCookie(true)
                    webViewCookieManager.setAcceptThirdPartyCookies(webView, true)
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            mainHandler.postDelayed({ finish(true) }, 200L)
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request.isForMainFrame) finish(false)
                        }
                    }
                    webView.loadUrl("$ORIGIN/api/auth/me", mapOf("Accept" to "application/json"))
                } catch (_: Throwable) {
                    finish(false)
                }
            }

            try {
                if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                    mainHandler.post { finish(false) }
                }
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
                mainHandler.post { finish(false) }
            }
            syncFromWebViewCookieManager()
            return refreshed.get()
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

    data class AuthCheckResult(
        val success: Boolean,
        val statusCode: Int,
        val cookieLength: Int,
        val hadCookie: Boolean,
        val body: String,
    )
}

