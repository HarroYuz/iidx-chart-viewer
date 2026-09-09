package com.harroyuz.iidxchartviewer.data.remote.bjm

import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import com.harroyuz.iidxchartviewer.BuildConfig
import okhttp3.Cookie
import okhttp3.Response

/** Opt in with adb shell setprop log.tag.BjmAuthDiag DEBUG. Never log credentials or response bodies. */
internal object BjmAuthDiagnostics {
    private const val TAG = "BjmAuthDiag"
    val enabled: Boolean get() = BuildConfig.DEBUG && Log.isLoggable(TAG, Log.DEBUG)

    // Callers supply only fixed event names, booleans, counts and status codes.
    fun event(message: String) {
        if (enabled) Log.d(TAG, message)
    }

    fun cookies(stage: String) {
        if (!enabled) return
        runCatching {
            val manager = CookieManager.getInstance()
            val origin = BjmSessionManager.ORIGIN
            fun present(path: String) = !manager.getCookie(origin + path).isNullOrBlank()
            event("cookies stage=$stage any=${manager.hasCookies()} root=${present("/")} auth=${present("/api/auth/me")} scores=${present("/api/WebUI/GetIidxScores")}")
        }.onFailure { event("cookies stage=$stage observation_failed=true") }
    }

    fun page(url: String): String = runCatching {
        val uri = Uri.parse(url)
        if (uri.host != BjmSessionManager.HOST) "EXTERNAL" else when (uri.path) {
            "/", "" -> "HOME"
            "/login" -> "LOGIN"
            "/api/auth/me" -> "AUTH"
            else -> "BJM_OTHER"
        }
    }.getOrDefault("UNKNOWN")

    fun response(response: Response) {
        if (!enabled) return
        runCatching {
            var hop: Response? = response
            var depth = 0
            while (hop != null) {
                val current = hop
                val sameHost = current.request.url.host == BjmSessionManager.HOST
                val values = if (sameHost) Cookie.parseAll(current.request.url, current.headers) else emptyList()
                val now = System.currentTimeMillis()
                val livePersistent = values.filter { it.persistent && it.expiresAt > now }
                val minLifetimeSeconds = livePersistent.minOfOrNull { (it.expiresAt - now) / 1000 } ?: -1L
                event("response depth=$depth bjm=$sameHost status=${current.code} setCookieCount=${values.size} sessionCount=${values.count { !it.persistent }} deletionCount=${values.count { it.expiresAt <= now }} minLifetimeSeconds=$minLifetimeSeconds")
                depth++
                hop = current.priorResponse
            }
        }.onFailure { event("response observation_failed=true") }
    }
}
