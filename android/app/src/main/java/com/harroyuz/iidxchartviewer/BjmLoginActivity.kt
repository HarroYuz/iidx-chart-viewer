package com.harroyuz.iidxchartviewer

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.concurrent.Executors

class BjmLoginActivity : Activity() {
    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private val probeExecutor = Executors.newSingleThreadExecutor()
    private var loginCompleted = false
    @Volatile private var probeInFlight = false
    private val authPoll = object : Runnable {
        override fun run() {
            if (loginCompleted || !::webView.isInitialized) return
            if (probeInFlight) {
                handler.postDelayed(this, AUTH_PROBE_INTERVAL_MS)
                return
            }
            probeInFlight = true
            val currentPoll = this
            probeExecutor.execute {
                val authenticated = runCatching {
                    if (!hasWebViewCookies()) null else BjmClient().probeAuthMe()
                }.getOrNull() != null
                runOnUiThread {
                    probeInFlight = false
                    if (authenticated) completeLogin()
                    else if (!loginCompleted) handler.postDelayed(currentPoll, AUTH_PROBE_INTERVAL_MS)
                }
            }
        }
    }

    private companion object {
        const val AUTH_PROBE_INTERVAL_MS = 1_200L
        const val COOKIE_SYNC_RETRY_COUNT = 8
        const val COOKIE_SYNC_RETRY_DELAY_MS = 120L
        const val ORIGIN = "https://u.bjmania.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "登录 BJMANIA"

        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = settings.userAgentString + " IIDXChartViewer/0.1"
            cookies.setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    handler.removeCallbacks(authPoll)
                    handler.post(authPoll)
                }
            }
        }
        setContentView(webView)
        webView.loadUrl("https://u.bjmania.com/login")
    }

    private fun hasWebViewCookies(): Boolean {
        val cookies = CookieManager.getInstance()
        cookies.flush()
        repeat(COOKIE_SYNC_RETRY_COUNT) {
            if (!cookies.getCookie(ORIGIN).isNullOrBlank()) return true
            SystemClock.sleep(COOKIE_SYNC_RETRY_DELAY_MS)
        }
        return !cookies.getCookie(ORIGIN).isNullOrBlank()
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    private fun completeLogin() {
        if (loginCompleted) return
        loginCompleted = true
        handler.removeCallbacks(authPoll)
        CookieManager.getInstance().flush()
        setResult(RESULT_OK)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(authPoll)
        probeExecutor.shutdownNow()
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
