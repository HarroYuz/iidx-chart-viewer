package com.harroyuz.iidxchartviewer.data.remote.textage

import com.harroyuz.iidxchartviewer.BuildConfig
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.model.TextageChartData
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Textage is treated as a data source only. This class never creates a WebView
 * or an ACTION_VIEW intent; the returned JavaScript data tables are parsed by
 * the Android app.
 */
class TextageClient {
    private val catalogUrl = "https://textage.cc/score/"

    suspend fun fetchCatalog(onProgress: suspend (completed: Int, total: Int, currentTitle: String) -> Unit = { _, _, _ -> }): List<IidxChart> = withContext(Dispatchers.IO) {
        val html = getHtml(catalogUrl)
        val scripts = TextageParser.scriptUrls(html, catalogUrl).mapNotNull { url ->
            runCatching { url to getHtmlWithRetry(url) }.getOrNull()
        }
        val result = TextageParser.parseCatalog(scripts.joinToString("\n") { it.second }, onProgress)
        if (result.size < 7_000) {
            throw TextageException("Textage 元数据不完整（仅识别到 ${result.size} 张谱面），请稍后重试")
        }
        result
    }

    internal suspend fun fetchChartPage(chart: IidxChart): TextageChartPage = withContext(Dispatchers.IO) {
        val baseUrl = chart.textageUrl?.substringBefore('?')
            ?: throw TextageException("该谱面没有可用的 Textage 链接")
        val pageUrl = buildTextageChartUrl(baseUrl, chart)
        val page = getHtmlWithRetry(pageUrl)
        val scripts = TextageParser.scriptUrls(page, pageUrl).mapNotNull { scriptUrl ->
            runCatching { getHtmlWithRetry(scriptUrl) }.getOrNull()
        }
        TextageChartPage(baseUrl, page, scripts)
    }

    internal fun parseChart(page: TextageChartPage, chart: IidxChart): TextageChartData =
        TextageParser.parseChart(
            chart = chart,
            source = page.html,
            externalScripts = page.externalScripts,
            pageUrl = buildTextageChartUrl(page.baseUrl, chart),
        )

    private fun getHtmlWithRetry(url: String, attempts: Int = 3): String {
        var lastError: Exception? = null
        repeat(attempts) {
            try {
                return getHtml(url)
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: TextageException("Textage 请求失败")
    }

    private fun getHtml(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            useCaches = false
            doInput = true
            setRequestProperty("Accept", "text/html,application/xhtml+xml,text/plain,*/*;q=0.8")
            setRequestProperty("Accept-Language", "ja,en-US;q=0.8,en;q=0.6")
            setRequestProperty("User-Agent", "IIDXChartViewer/${BuildConfig.VERSION_NAME} (Android)")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            if (code !in 200..299) throw TextageException("Textage 请求失败 ($code)")
            decodeHtml(bytes, connection.contentType)
        } finally {
            connection.disconnect()
        }
    }

    private fun decodeHtml(bytes: ByteArray, contentType: String?): String {
        val headerCharset = Regex("(?i)charset\\s*=\\s*([\\w-]+)").find(contentType.orEmpty())?.groupValues?.get(1)
        val probe = bytes.copyOfRange(0, bytes.size.coerceAtMost(4096)).toString(Charsets.ISO_8859_1)
        val metaCharset = Regex("(?i)charset\\s*=\\s*[\\\"']?([\\w-]+)").find(probe)?.groupValues?.get(1)
        val charset = runCatching { Charset.forName(headerCharset ?: metaCharset ?: "Shift_JIS") }
            .getOrDefault(Charsets.UTF_8)
        return bytes.toString(charset)
    }
}

internal data class TextageChartPage(
    val baseUrl: String,
    val html: String,
    val externalScripts: List<String>,
)

class TextageException(message: String) : Exception(message)
