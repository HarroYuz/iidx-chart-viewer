package com.harroyuz.iidxchartviewer

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class BjmLoginActivity : Activity() {
    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private var loginCompleted = false
    private val authPoll = object : Runnable {
        override fun run() {
            if (loginCompleted || !::webView.isInitialized) return
            webView.evaluateJavascript(
                "(async()=>{try{const r=await fetch('/api/auth/me',{credentials:'include',cache:'no-store'});return r.ok?'AUTH_OK':'AUTH_WAIT'}catch(e){return 'AUTH_WAIT'}})();",
            ) { result ->
                if (result == "\"AUTH_OK\"") completeLogin()
                else if (!loginCompleted) handler.postDelayed(this, 700L)
            }
        }
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
                    if (url.startsWith("https://u.bjmania.com/")) handler.removeCallbacks(authPoll)
                    handler.post(authPoll)
                }
            }
        }
        setContentView(webView)
        webView.loadUrl("https://u.bjmania.com/login")
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
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
