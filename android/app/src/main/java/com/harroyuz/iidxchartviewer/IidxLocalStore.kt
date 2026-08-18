package com.harroyuz.iidxchartviewer

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class IidxLocalStore(context: Context) {
    private companion object {
        const val CHART_CACHE_VERSION = 7
        const val CATALOG_HEADER = "#iidx-catalog-v3"
        const val TEXTAGE_CATALOG_PARSER_VERSION = 2
    }

    private val preferences = context.getSharedPreferences("iidx-local-state", Context.MODE_PRIVATE)
    private val catalogFile = File(context.filesDir, "textage-catalog.tsv")
    private val chartCacheDirectory = File(context.filesDir, "textage-chart-cache")

    fun load(): IidxAppState {
        val confirmed = preferences.getStringSet("confirmed", emptySet()).orEmpty()
        val charts = runCatching {
            if (catalogFile.isFile) {
                val local = readCatalogFile()
                if (local.isNotEmpty() && catalogFile.readLines().firstOrNull() != CATALOG_HEADER) writeCatalogFile(local)
                local
            } else {
                val array = JSONArray(preferences.getString("charts", "[]"))
                val legacy = buildList {
                    for (index in 0 until array.length()) {
                        val chart = array.getJSONObject(index).toChart()
                        if (!chart.id.startsWith("demo-")) add(chart)
                    }
                }
                if (legacy.isNotEmpty()) writeCatalogFile(legacy)
                legacy
            }
        }.getOrElse { emptyList() }
            .map { chart -> chart.copy(confirmed = chart.confirmed || chart.id in confirmed) }
        val scores = runCatching {
            val array = JSONArray(preferences.getString("bjm_scores", "[]"))
            buildList {
                for (index in 0 until array.length()) add(array.getJSONObject(index).toScore())
            }
        }.getOrDefault(emptyList())
        val user = preferences.getString("bjm_user", null)?.let {
            runCatching {
                val json = JSONObject(it)
                BjmUser(json.optString("id"), json.optString("name"), json.optString("email"))
            }.getOrNull()
        }
        return IidxAppState(
            charts = charts,
            bjmScores = scores,
            bjmUser = user,
            bjmSyncedAt = preferences.getLong("bjm_synced_at", 0L).takeIf { it > 0L },
        )
    }

    fun save(state: IidxAppState) {
        writeCatalogFile(state.charts)
        val scores = JSONArray().apply { state.bjmScores.forEach { put(it.toJson()) } }
        val user = state.bjmUser?.let {
            JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("email", it.email)
            }.toString()
        }
        preferences.edit()
            .remove("charts")
            .putBoolean("textage_catalog_present", state.charts.isNotEmpty())
            .putStringSet("confirmed", state.charts.filter { it.confirmed }.map { it.id }.toSet())
            .putString("bjm_scores", scores.toString())
            .putString("bjm_user", user)
            .putLong("bjm_synced_at", state.bjmSyncedAt ?: 0L)
            .apply()
    }

    fun hasTextageCatalog(): Boolean = preferences.getBoolean("textage_catalog_present", false) && load().charts.isNotEmpty()

    fun hasTextageCatalogMarker(): Boolean = preferences.getBoolean("textage_catalog_present", false)

    fun isTextageSyncComplete(): Boolean =
        preferences.getBoolean("textage_sync_complete", false) &&
            preferences.getInt("textage_catalog_parser_version", 0) >= TEXTAGE_CATALOG_PARSER_VERSION

    fun setTextageSyncComplete(complete: Boolean) {
        preferences.edit()
            .putBoolean("textage_sync_complete", complete)
            .putInt("textage_catalog_parser_version", if (complete) TEXTAGE_CATALOG_PARSER_VERSION else 0)
            .apply()
    }

    fun textageLastSyncAt(): Long = preferences.getLong("textage_last_sync_at", 0L)

    fun setTextageLastSyncAt(timestamp: Long = System.currentTimeMillis()) {
        preferences.edit().putLong("textage_last_sync_at", timestamp).apply()
    }

    fun autoUpdateEnabled(): Boolean = preferences.getBoolean("auto_update_enabled", true)

    fun setAutoUpdateEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("auto_update_enabled", enabled).apply()
    }

    fun updateLastCheckAt(): Long = preferences.getLong("update_last_check_at", 0L)

    fun setUpdateLastCheckAt(timestamp: Long = System.currentTimeMillis()) {
        preferences.edit().putLong("update_last_check_at", timestamp).apply()
    }

    fun loadPlayerSettings(): PlayerSettings {
        val speed = loadPlayerSpeed()
        val legacyOption = preferences.getString("player_option", null)
            ?.takeIf(::isPlayerOption)
            ?: if (preferences.getBoolean("player_mirror", false)) "MIRROR" else "NONE"
        val option1P = preferences.getString("player_option_1p", null)
            ?.takeIf(::isPlayerOption)
            ?: legacyOption
        val option2P = preferences.getString("player_option_2p", null)
            ?.takeIf(::isPlayerOption)
            ?: legacyOption
        return PlayerSettings(
        speed = speed,
        showBarLines = preferences.getBoolean("player_show_bar_lines", true),
        showBpmChanges = preferences.getBoolean("player_show_bpm_changes", true),
        showMeasureNumbers = preferences.getBoolean("player_show_measure_numbers", true),
        side = preferences.getString("player_side", "1P")?.takeIf { it == "1P" || it == "2P" } ?: "1P",
        playOption = legacyOption,
        playOption1P = option1P,
        playOption2P = option2P,
        randomMapping1P = loadRandomMapping("player_random_1p"),
        randomMapping2P = loadRandomMapping("player_random_2p"),
        )
    }

    private fun loadPlayerSpeed(): Int {
        val migrated = if (preferences.getInt("player_speed_scale_version", 0) >= 2) {
            preferences.getInt("player_speed", 1)
        } else {
            val oldSpeed = preferences.getInt("player_speed", 4)
            // The new control stores a compact value: 1 means the old 4x
            // visual speed. Normalize both the original storage and the
            // intermediate 0.2.0 migration back to that representation.
            val nextSpeed = if (preferences.contains("player_speed")) {
                (oldSpeed + 3) / 4
            } else {
                1
            }
            preferences.edit()
                .putInt("player_speed", nextSpeed.coerceIn(1, 100))
                .putInt("player_speed_scale_version", 2)
                .remove("player_speed_v2")
                .apply()
            nextSpeed
        }
        return migrated.coerceIn(1, 100)
    }

    fun savePlayerSettings(settings: PlayerSettings) {
        preferences.edit()
            .putInt("player_speed", settings.safeSpeed)
            .putBoolean("player_show_bar_lines", settings.showBarLines)
            .putBoolean("player_show_bpm_changes", settings.showBpmChanges)
            .putBoolean("player_show_measure_numbers", settings.showMeasureNumbers)
            .putString("player_side", settings.side)
            .putString("player_option", settings.safePlayOption)
            .putString("player_option_sp", settings.safePlayOption)
            .putString("player_option_1p", settings.safePlayOption1P)
            .putString("player_option_2p", settings.safePlayOption2P)
            .putBoolean("player_mirror", settings.safePlayOption == "MIRROR")
            .putInt("player_speed_scale_version", 2)
            .putString("player_random_1p", settings.safeRandomMapping1P.joinToString(","))
            .putString("player_random_2p", settings.safeRandomMapping2P.joinToString(","))
            .apply()
    }

    private fun isPlayerOption(value: String): Boolean = value == "NONE" || value == "MIRROR" || value == "RANDOM"

    private fun loadRandomMapping(key: String): List<Int> = preferences.getString(key, null)
        ?.split(',')
        ?.mapNotNull { it.toIntOrNull() }
        ?.takeIf { it.size == 7 && it.toSet() == (1..7).toSet() }
        ?: (1..7).toList()

    fun hasChartData(chartId: String): Boolean = chartCacheFile(chartId).isFile

    fun loadChartData(chart: IidxChart): TextageChartData? = runCatching {
        val json = JSONObject(chartCacheFile(chart.id).readText())
        if (json.optInt("parser_version", 0) != CHART_CACHE_VERSION) return@runCatching null
        val array = json.optJSONArray("notes") ?: JSONArray()
        val notes = buildList {
            for (index in 0 until array.length()) {
                val note = array.getJSONObject(index)
                add(
                    ChartNote(
                        beat = note.optDouble("beat").toFloat(),
                        lane = note.optInt("lane"),
                        holdBeats = note.optDouble("hold_beats").toFloat(),
                    ),
                )
            }
        }
        val bpmChanges = buildList {
            val changes = json.optJSONArray("bpm_changes") ?: JSONArray()
            for (index in 0 until changes.length()) {
                val change = changes.optJSONObject(index) ?: continue
                add(BpmChange(change.optDouble("beat").toFloat(), change.optDouble("bpm").toFloat()))
            }
        }
        val measureLengths = buildList {
            val lengths = json.optJSONArray("measure_lengths") ?: JSONArray()
            for (index in 0 until lengths.length()) add(lengths.optDouble(index, 4.0).toFloat())
        }
        TextageChartData(
            chart = chart.copy(notes = json.optInt("notes_count", chart.notes)),
            notes = notes,
            durationBeats = json.optDouble("duration_beats").toFloat(),
            parsed = json.optBoolean("parsed"),
            bpm = json.optDouble("bpm", 150.0).toFloat(),
            bpmChanges = bpmChanges,
            measureLengths = measureLengths,
            parserMessage = json.optString("parser_message").takeIf { it.isNotBlank() },
        )
    }.getOrNull()

    fun saveChartData(data: TextageChartData) {
        if (!chartCacheDirectory.exists()) chartCacheDirectory.mkdirs()
        val notes = JSONArray().apply {
            data.notes.forEach { note ->
                put(
                    JSONObject().apply {
                        put("beat", note.beat)
                        put("lane", note.lane)
                        put("hold_beats", note.holdBeats)
                    },
                )
            }
        }
        val json = JSONObject().apply {
            put("parser_version", CHART_CACHE_VERSION)
            put("notes", notes)
            put("notes_count", data.chart.notes)
            put("duration_beats", data.durationBeats)
            put("parsed", data.parsed)
            put("bpm", data.bpm)
            put("bpm_changes", JSONArray().apply {
                data.bpmChanges.forEach { change ->
                    put(JSONObject().apply {
                        put("beat", change.beat)
                        put("bpm", change.bpm)
                    })
                }
            })
            put("measure_lengths", JSONArray().apply {
                data.measureLengths.forEach { put(it) }
            })
            put("parser_message", data.parserMessage ?: JSONObject.NULL)
        }
        chartCacheFile(data.chart.id).writeText(json.toString())
    }

    fun clear() {
        preferences.edit().clear().apply()
        catalogFile.delete()
        chartCacheDirectory.listFiles()?.forEach { it.delete() }
    }

    private fun readCatalogFile(): List<IidxChart> = catalogFile.useLines { lines ->
        val allLines = lines.toList()
        val compact = allLines.firstOrNull() == CATALOG_HEADER
        allLines.drop(if (compact) 1 else 0).mapNotNull { line ->
            val values = line.split('\t')
            if (values.size != 14) return@mapNotNull null
            runCatching {
                val text = if (compact) ::unescapeField else ::decodeField
                val number = if (compact) String::toInt else ::decodeIntField
                IidxChart(
                    id = text(values[0]),
                    title = text(values[1]),
                    subtitle = text(values[2]),
                    genre = text(values[3]),
                    composer = text(values[4]),
                    bpm = text(values[5]),
                    mode = text(values[6]),
                    difficulty = text(values[7]),
                    level = number(values[8]),
                    notes = number(values[9]),
                    version = text(values[10]),
                    score = values[11].takeIf { it.isNotEmpty() }?.let(number),
                    confirmed = number(values[12]) == 1,
                    textageUrl = values[13].takeIf { it.isNotEmpty() }?.let(text),
                )
            }.getOrNull()
        }.toList()
    }

    private fun writeCatalogFile(charts: List<IidxChart>) {
        catalogFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.append(CATALOG_HEADER)
            writer.newLine()
            charts.filterNot { it.id.startsWith("demo-") }.forEachIndexed { index, chart ->
                if (index > 0) writer.newLine()
                writer.append(
                    listOf(
                        escapeField(chart.id),
                        escapeField(chart.title),
                        escapeField(chart.subtitle),
                        escapeField(chart.genre),
                        escapeField(chart.composer),
                        escapeField(chart.bpm),
                        escapeField(chart.mode),
                        escapeField(chart.difficulty),
                        chart.level.toString(),
                        chart.notes.toString(),
                        escapeField(chart.version),
                        chart.score?.toString().orEmpty(),
                        if (chart.confirmed) "1" else "0",
                        chart.textageUrl?.let(::escapeField).orEmpty(),
                    ).joinToString("\t"),
                )
            }
        }
    }

    private fun escapeField(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(char)
            }
        }
    }

    private fun unescapeField(value: String): String = buildString(value.length) {
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '\\' && index + 1 < value.length) {
                when (val escaped = value[index + 1]) {
                    't' -> append('\t')
                    'n' -> append('\n')
                    'r' -> append('\r')
                    '\\' -> append('\\')
                    else -> {
                        append('\\')
                        append(escaped)
                    }
                }
                index += 2
            } else {
                append(char)
                index++
            }
        }
    }

    private fun encodeField(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun decodeField(value: String): String = String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8)

    private fun decodeIntField(value: String): Int = value.toIntOrNull() ?: decodeField(value).toInt()

    private fun chartCacheFile(chartId: String): File = File(
        chartCacheDirectory,
        chartId.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".json",
    )
}

private fun IidxChart.toJson() = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("subtitle", subtitle)
    put("genre", genre)
    put("composer", composer)
    put("bpm", bpm)
    put("mode", mode)
    put("difficulty", difficulty)
    put("level", level)
    put("notes", notes)
    put("version", version)
    put("score", score ?: JSONObject.NULL)
    put("confirmed", confirmed)
    put("textage_url", textageUrl ?: JSONObject.NULL)
}

private fun JSONObject.toChart() = IidxChart(
    id = optString("id"),
    title = optString("title"),
    subtitle = optString("subtitle"),
    genre = optString("genre"),
    composer = optString("composer", optString("artist")),
    bpm = optString("bpm"),
    mode = optString("mode", "SP"),
    difficulty = optString("difficulty", "N"),
    level = optInt("level"),
    notes = optInt("notes"),
    version = optString("version", "Textage"),
    score = if (isNull("score")) null else optInt("score"),
    confirmed = optBoolean("confirmed"),
    textageUrl = if (isNull("textage_url")) null else optString("textage_url").takeIf { it.isNotBlank() },
)

private fun BjmScore.toJson() = JSONObject().apply {
    put("music_id", musicId)
    put("play_style", playStyle)
    put("note_id", noteId)
    put("clear_flag", clearFlag)
    put("miss_count", missCount)
    put("time", time)
    put("ex_score", exScore)
    put("option1", option1)
    put("option2", option2)
}

private fun JSONObject.toScore() = BjmScore(
    musicId = optInt("music_id"),
    playStyle = optInt("play_style"),
    noteId = optInt("note_id"),
    clearFlag = optInt("clear_flag"),
    missCount = optInt("miss_count"),
    time = optLong("time"),
    exScore = optInt("ex_score"),
    option1 = optLong("option1"),
    option2 = optLong("option2"),
)
