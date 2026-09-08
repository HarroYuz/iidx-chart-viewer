package com.harroyuz.iidxchartviewer.data.remote.bjm

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

internal class BjmCookieJar : CookieJar {
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
