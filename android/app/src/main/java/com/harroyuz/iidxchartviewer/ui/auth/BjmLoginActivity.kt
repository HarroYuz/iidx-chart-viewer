package com.harroyuz.iidxchartviewer.ui.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.harroyuz.iidxchartviewer.BuildConfig
import com.harroyuz.iidxchartviewer.data.remote.bjm.BjmClient
import com.harroyuz.iidxchartviewer.ui.components.AppTopBar
import com.harroyuz.iidxchartviewer.ui.theme.IidxTheme
import com.harroyuz.iidxchartviewer.ui.theme.configureSystemBars
import java.util.concurrent.Executors

class BjmLoginActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private val probeExecutor = Executors.newSingleThreadExecutor()
    private var loginCompleted = false
    @Volatile private var destroyed = false
    @Volatile private var probeInFlight = false
    private val authPoll = object : Runnable {
        override fun run() {
            if (destroyed || loginCompleted || !::webView.isInitialized) return
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
                    if (destroyed) return@runOnUiThread
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
        configureSystemBars()
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
            settings.userAgentString = settings.userAgentString + " IIDXChartViewer/${BuildConfig.VERSION_NAME}"
            cookies.setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    handler.removeCallbacks(authPoll)
                    handler.post(authPoll)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this) { navigateBack() }
        setContent {
            IidxTheme {
                Scaffold { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        AppTopBar(title = "登录 BJM", onNavigate = ::navigateBack)
                        AndroidView(factory = { webView }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
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

    private fun navigateBack() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else finish()
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
        destroyed = true
        handler.removeCallbacks(authPoll)
        probeExecutor.shutdownNow()
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
