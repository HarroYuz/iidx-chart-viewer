package com.harroyuz.iidxchartviewer.data.remote.bjm

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import com.harroyuz.iidxchartviewer.domain.model.BjmChartMetadata
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** Public assets use their own client; this repository never reads or changes login cookies. */
internal class BjmChartMetadataRepository(context: Context) {
    private val cache = AtomicFile(File(context.filesDir, "bjm_chart_metadata_v1.json"))
    private val mutex = Mutex()
    private val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
    private var cachedJson: JSONObject? = null
    private var cachedData: BjmChartMetadata? = null

    suspend fun loadCached(): BjmChartMetadata? = withContext(Dispatchers.IO) {
        mutex.withLock { loadDisk() }
    }

    private fun loadDisk(): BjmChartMetadata? {
        if (cachedData != null) return cachedData
        return runCatching {
            val json = cache.openRead().use { JSONObject(it.readBytes().toString(Charsets.UTF_8)) }
            decode(json).also { cachedJson = json; cachedData = it }
        }.getOrNull()
    }

    suspend fun refresh(): BjmChartMetadata = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadDisk()
            val now = System.currentTimeMillis()
            val old = cachedJson
            if (cachedData != null && old != null && now - old.optLong("checkedAt") in 0 until 86_400_000L) {
                return@withLock cachedData!!
            }
            val versions = JSONObject(fetch("ver.json").toString(Charsets.UTF_8)).getJSONObject("LDJ")
            val radarVersions = versions.getJSONObject("radar")
            val noteVersions = versions.getJSONObject("notecount")
            val version = radarVersions.keys().asSequence().mapNotNull(String::toIntOrNull)
                .maxOrNull() ?: throw BjmException("BJM 没有雷达数据版本")
            val noteVersion = noteVersions.keys().asSequence().mapNotNull(String::toIntOrNull)
                .maxOrNull() ?: throw BjmException("BJM 没有 NOTE 数据版本")
            val radarRevision = radarVersions.getString(version.toString())
            val noteRevision = noteVersions.getString(noteVersion.toString())
            val unchanged = old != null && old.optInt("version") == version &&
                old.optInt("noteVersion", version) == noteVersion &&
                old.optString("radarRevision") == radarRevision && old.optString("noteRevision") == noteRevision
            val next = if (unchanged) JSONObject(old.toString()) else JSONObject().apply {
                put("version", version)
                put("noteVersion", noteVersion)
                put("radarRevision", radarRevision)
                put("noteRevision", noteRevision)
                put("radar", Base64.encodeToString(fetch("LDJ_radar_$version.bin"), Base64.NO_WRAP))
                put("notes", Base64.encodeToString(fetch("LDJ_notecount_$noteVersion.bin"), Base64.NO_WRAP))
            }
            next.put("checkedAt", now)
            val data = if (unchanged) cachedData!! else decode(next)
            val output = cache.startWrite()
            try {
                output.write(next.toString().toByteArray(Charsets.UTF_8))
                cache.finishWrite(output)
            } catch (error: Exception) {
                cache.failWrite(output)
                throw error
            }
            cachedJson = next
            cachedData = data
            data
        }
    }

    private fun decode(json: JSONObject): BjmChartMetadata {
        val radars = BjmChartMetadataProto.decodeRadars(Base64.decode(json.getString("radar"), Base64.DEFAULT))
        val notes = BjmChartMetadataProto.decodeNoteCounts(Base64.decode(json.getString("notes"), Base64.DEFAULT))
        if (radars.isEmpty() || notes.isEmpty()) throw BjmException("BJM 谱面数据库为空")
        return BjmChartMetadata(json.getInt("version"), radars, notes, json.optInt("noteVersion", json.getInt("version")))
    }

    private fun fetch(path: String): ByteArray = client.newCall(
        Request.Builder().url("https://assets.bjmania.com/mdb/$path").build(),
    ).execute().use { response ->
        if (!response.isSuccessful) throw BjmException("BJM 谱面数据库下载失败：HTTP ${response.code}")
        response.body?.bytes() ?: throw BjmException("BJM 谱面数据库返回为空")
    }
}
