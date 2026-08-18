package com.harroyuz.iidxchartviewer

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GithubReleaseInfo(
    val tagName: String,
    val title: String,
    val notes: String,
    val apkName: String,
    val apkUrl: String,
)

class GithubUpdateClient {
    suspend fun fetchLatestRelease(): GithubReleaseInfo = withContext(Dispatchers.IO) {
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            useCaches = false
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "IIDXData/$APP_VERSION")
        }
        try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (code !in 200..299) throw IllegalStateException("GitHub 更新检查失败 ($code)")
            val json = JSONObject(body)
            val tagName = json.optString("tag_name").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("GitHub Release 缺少版本号")
            val assets = json.optJSONArray("assets") ?: JSONObject.NULL
            var apkName: String? = null
            var apkUrl: String? = null
            if (assets is org.json.JSONArray) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    val url = asset.optString("browser_download_url")
                    if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                        apkName = name
                        apkUrl = url
                        break
                    }
                }
            }
            GithubReleaseInfo(
                tagName = tagName,
                title = json.optString("name").ifBlank { tagName },
                notes = json.optString("body").trim(),
                apkName = apkName ?: "iidx_data_${tagName.removePrefix("v")}.apk",
                apkUrl = apkUrl ?: throw IllegalStateException("该 Release 没有可安装的 APK")
            )
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadApk(
        context: Context,
        release: GithubReleaseInfo,
        onProgress: suspend (downloaded: Long, total: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw IllegalStateException("无法创建 APK 下载目录")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("无法创建 APK 下载目录")
        }
        val safeName = release.apkName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val target = File(directory, safeName)
        val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            useCaches = false
            setRequestProperty("User-Agent", "IIDXData/$APP_VERSION")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("APK 下载失败 ($code)")
            val total = connection.contentLengthLong
            var downloaded = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        onProgress(downloaded, total)
                    }
                }
            }
            target
        } catch (error: Exception) {
            target.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/HarroYuz/iidx-chart-viewer/releases/latest"
        private const val APP_VERSION = BuildConfig.VERSION_NAME

        fun isNewer(current: String, latest: String): Boolean = compareVersions(latest, current) > 0

        private fun compareVersions(left: String, right: String): Int {
            val leftParts = versionParts(left)
            val rightParts = versionParts(right)
            for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
                val l = leftParts.getOrElse(index) { 0 }
                val r = rightParts.getOrElse(index) { 0 }
                if (l != r) return l.compareTo(r)
            }
            return 0
        }

        private fun versionParts(value: String): List<Int> = Regex("\\d+")
            .findAll(value)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()
            .ifEmpty { listOf(0) }
    }
}
