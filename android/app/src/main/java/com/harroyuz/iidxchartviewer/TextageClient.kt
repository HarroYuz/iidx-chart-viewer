package com.harroyuz.iidxchartviewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.Locale

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

    suspend fun fetchChart(chart: IidxChart): TextageChartData = withContext(Dispatchers.IO) {
        val pageUrl = chart.textageUrl?.let { chartPageUrl(it, chart) }
            ?: throw TextageException("该谱面没有可用的 Textage 链接")
        val page = getHtmlWithRetry(pageUrl)
        TextageParser.parseChart(chart, page)
    }

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

    private fun chartPageUrl(baseUrl: String, chart: IidxChart): String {
        val mode = if (chart.mode == "DP") "D" else "s"
        val difficulty = when (chart.difficulty) {
            "A" -> "A"
            "L" -> "X"
            "N" -> "N"
            "B" -> "B"
            else -> "H"
        }
        return baseUrl + if (baseUrl.contains('?')) "&$mode$difficulty" else "?$mode$difficulty"
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
            setRequestProperty("User-Agent", "IIDXChartViewer/0.1 (Android)")
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

class TextageException(message: String) : Exception(message)

internal object TextageParser {
    private const val TEXTAGE_BAR_TICKS = 384
    private const val TEXTAGE_QUARTER_TICKS = 96f
    private const val TEXTAGE_POSITION_TICKS = 32f
    private val tagPattern = Regex("(?is)<[^>]+>")
    private val scriptPattern = Regex("(?is)<script\\b[^>]*>")
    private val chartSlots = listOf(
        ChartSlot("SP", "B", "sbo", dataIndex = 0, levelIndex = 1),
        ChartSlot("SP", "B", "sb", dataIndex = 1, levelIndex = 3),
        ChartSlot("SP", "N", "n", dataIndex = 2, levelIndex = 5),
        ChartSlot("SP", "H", "h", dataIndex = 3, levelIndex = 7),
        ChartSlot("SP", "A", "a", dataIndex = 4, levelIndex = 9),
        ChartSlot("SP", "L", "l", dataIndex = 5, levelIndex = 11),
        ChartSlot("DP", "B", "b", dataIndex = 6, levelIndex = 13),
        ChartSlot("DP", "N", "n", dataIndex = 7, levelIndex = 15),
        ChartSlot("DP", "H", "h", dataIndex = 8, levelIndex = 17),
        ChartSlot("DP", "A", "a", dataIndex = 9, levelIndex = 19),
        ChartSlot("DP", "L", "l", dataIndex = 10, levelIndex = 21),
    )

    private data class ChartSlot(
        val mode: String,
        val difficulty: String,
        val code: String,
        val dataIndex: Int,
        val levelIndex: Int,
    )

    private data class JsValue(val text: String, val quoted: Boolean) {
        fun intValue(): Int? {
            val normalized = text.trim()
            return normalized.toIntOrNull() ?: normalized.toIntOrNull(16)
        }
    }

    private data class ChargeEntry(
        val lane: Int,
        val position: Int,
        val length: Int = 30,
        val type: Int = 3,
    )

    private data class OpenCharge(
        val startBeat: Float,
        var lastBeat: Float,
    )

    suspend fun parseCatalog(
        source: String,
        onProgress: suspend (completed: Int, total: Int, currentTitle: String) -> Unit,
    ): List<IidxChart> {
        val titleEntries = parseObjectEntries(source, "titletbl")
        val noteEntries = parseObjectEntries(source, "datatbl").associateBy { it.first }
        val levelEntries = LinkedHashMap<String, List<JsValue>>()
        parseObjectEntries(source, "actbl").forEach { levelEntries[it.first] = it.second }
        parseIndexedObjects(source, "cstbl").forEach { (key, values) ->
            levelEntries[key] = mergeChartValues(levelEntries[key], values)
        }
        val versions = parseStringArray(source, "vertbl")
        val result = ArrayList<IidxChart>()
        val total = titleEntries.size
        for ((index, entry) in titleEntries.withIndex()) {
            val key = entry.first
            val values = entry.second
            val title = values.getOrNull(5)?.text.orEmpty().let(::cleanJsText)
            if (title.isBlank()) {
                onProgress(index + 1, total, key)
                continue
            }
            val subtitle = values.getOrNull(6)?.text.orEmpty().let(::cleanJsText)
            val genre = values.getOrNull(3)?.text.orEmpty().let(::cleanJsText)
            val composer = values.getOrNull(4)?.text.orEmpty().let(::cleanJsText)
            val versionIndex = values.getOrNull(0)?.intValue() ?: 0
            val version = versions.getOrNull(versionIndex)?.ifBlank { null } ?: "Textage"
            val notes = noteEntries[key]?.second.orEmpty()
            val levels = levelEntries[key].orEmpty()
            val bpm = notes.lastOrNull()?.text.orEmpty().let(::cleanJsText)
            // Version 0 is a real Textage directory (for example
            // /score/0/chocopla.html), not a missing-version marker. A chart
            // with zero notes still stays in the catalog as an unavailable
            // difficulty, but must not receive a clickable page URL.
            val chartBaseUrl = "https://textage.cc/score/${versionIndex.coerceAtLeast(0)}/${key}.html"
            chartSlots.forEach { slot ->
                val level = levels.getOrNull(slot.levelIndex)?.intValue() ?: 0
                if (level <= 0) return@forEach
                val noteCount = notes.getOrNull(slot.dataIndex)?.intValue() ?: 0
                result += IidxChart(
                    id = "textage-${slug(key)}-${slot.mode.lowercase(Locale.ROOT)}${slot.code.lowercase(Locale.ROOT)}",
                    title = title,
                    subtitle = subtitle,
                    genre = genre,
                    composer = composer,
                    bpm = bpm,
                    mode = slot.mode,
                    difficulty = slot.difficulty,
                    level = level,
                    notes = noteCount,
                    version = version,
                    textageUrl = chartBaseUrl.takeIf { noteCount > 0 },
                )
            }
            onProgress(index + 1, total, title)
        }
        // The same song key can appear once in the CS/Consumer table and
        // again in the AC table. They describe the same difficulty card; keep
        // the entry with a usable page and the richer metadata.
        return result.groupBy { it.id }.values.map { candidates ->
            candidates.maxWithOrNull(
                compareBy<IidxChart>({ it.textageUrl != null }, { it.notes }, { it.bpm.isNotBlank() }),
            ) ?: candidates.first()
        }
    }

    private fun parseObjectEntries(source: String, objectName: String): List<Pair<String, List<JsValue>>> {
        val marker = Regex("(?s)\\b${Regex.escape(objectName)}\\s*=\\s*\\{").find(source) ?: return emptyList()
        val openIndex = source.indexOf('{', marker.range.first)
        val closeIndex = balancedEnd(source, openIndex, '{', '}')
        if (openIndex < 0 || closeIndex <= openIndex) return emptyList()
        return parseObjectBody(source.substring(openIndex + 1, closeIndex))
    }

    private fun parseIndexedObjects(source: String, objectName: String): List<Pair<String, List<JsValue>>> {
        val result = LinkedHashMap<String, List<JsValue>>()
        val marker = Regex("(?s)\\b${Regex.escape(objectName)}\\s*\\[\\s*\\d+\\s*]\\s*=\\s*\\{")
        marker.findAll(source).forEach { match ->
            val openIndex = source.indexOf('{', match.range.first)
            val closeIndex = balancedEnd(source, openIndex, '{', '}')
            if (openIndex >= 0 && closeIndex > openIndex) {
                parseObjectBody(source.substring(openIndex + 1, closeIndex)).forEach { (key, values) ->
                    result[key] = mergeChartValues(result[key], values)
                }
            }
        }
        return result.toList()
    }

    private fun parseStringArray(source: String, variableName: String): List<String> {
        val marker = Regex("(?s)\\b${Regex.escape(variableName)}\\s*=\\s*\\[").find(source) ?: return emptyList()
        val openIndex = source.indexOf('[', marker.range.first)
        val closeIndex = balancedEnd(source, openIndex, '[', ']')
        if (openIndex < 0 || closeIndex <= openIndex) return emptyList()
        return parseJsValues(source.substring(openIndex + 1, closeIndex)).map { it.text }
    }

    private fun parseObjectBody(body: String): List<Pair<String, List<JsValue>>> {
        val result = ArrayList<Pair<String, List<JsValue>>>()
        var index = 0
        while (index < body.length) {
            index = skipWhitespaceAndComments(body, index)
            if (index >= body.length) break
            if (body[index] != '\'' && body[index] != '\"') {
                index++
                continue
            }
            val keyResult = readJsString(body, index)
            val key = keyResult.first
            index = skipWhitespaceAndComments(body, keyResult.second)
            if (index >= body.length || body[index] != ':') continue
            index = skipWhitespaceAndComments(body, index + 1)
            if (index >= body.length || body[index] != '[') continue
            val closeIndex = balancedEnd(body, index, '[', ']')
            if (closeIndex <= index) break
            result += key to parseJsValues(body.substring(index + 1, closeIndex))
            index = closeIndex + 1
        }
        return result
    }

    private fun parseJsValues(arrayBody: String): List<JsValue> {
        val result = ArrayList<JsValue>()
        var index = 0
        var tokenStart = 0
        var quote: Char? = null
        var escaped = false
        while (index < arrayBody.length) {
            val char = arrayBody[index]
            if (quote != null) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == quote) quote = null
            } else if (char == '\'' || char == '\"') {
                quote = char
            } else if (char == ',') {
                addJsValue(result, arrayBody.substring(tokenStart, index))
                tokenStart = index + 1
            }
            index++
        }
        addJsValue(result, arrayBody.substring(tokenStart))
        return result
    }

    private fun addJsValue(result: MutableList<JsValue>, token: String) {
        val trimmed = token.trim()
            .replace(Regex("(?s)/\\*.*?\\*/"), "")
            .trim()
        if (trimmed.isBlank()) return
        if (trimmed.firstOrNull() == '\'' || trimmed.firstOrNull() == '\"') {
            val parsed = readJsString(trimmed, 0)
            result += JsValue(parsed.first, quoted = true)
        } else {
            result += JsValue(trimmed, quoted = false)
        }
    }

    private fun readJsString(source: String, start: Int): Pair<String, Int> {
        val quote = source[start]
        val result = StringBuilder()
        var index = start + 1
        while (index < source.length) {
            val char = source[index]
            if (char == quote) return result.toString() to (index + 1)
            if (char == '\\' && index + 1 < source.length) {
                val escaped = source[index + 1]
                when (escaped) {
                    'n' -> result.append('\n')
                    'r' -> result.append('\r')
                    't' -> result.append('\t')
                    'x' -> {
                        val hex = source.substring(index + 2, (index + 4).coerceAtMost(source.length))
                        result.append(hex.toIntOrNull(16)?.toChar() ?: escaped)
                        index += 2
                    }
                    'u' -> {
                        val hex = source.substring(index + 2, (index + 6).coerceAtMost(source.length))
                        result.append(hex.toIntOrNull(16)?.toChar() ?: escaped)
                        index += 4
                    }
                    else -> result.append(escaped)
                }
                index += 2
                continue
            }
            result.append(char)
            index++
        }
        return result.toString() to source.length
    }

    private fun skipWhitespaceAndComments(source: String, start: Int): Int {
        var index = start
        while (index < source.length) {
            when {
                source[index].isWhitespace() -> index++
                source.startsWith("//", index) -> {
                    val lineEnd = source.indexOf('\n', index + 2)
                    index = if (lineEnd < 0) source.length else lineEnd + 1
                }
                source.startsWith("/*", index) -> {
                    val commentEnd = source.indexOf("*/", index + 2)
                    index = if (commentEnd < 0) source.length else commentEnd + 2
                }
                else -> return index
            }
        }
        return index
    }

    private fun balancedEnd(source: String, openIndex: Int, opening: Char, closing: Char): Int {
        if (openIndex < 0 || openIndex >= source.length || source[openIndex] != opening) return -1
        var depth = 0
        var quote: Char? = null
        var escaped = false
        var index = openIndex
        while (index < source.length) {
            val char = source[index]
            if (quote != null) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == quote) quote = null
            } else if (char == '\'' || char == '\"') {
                quote = char
            } else if (source.startsWith("//", index)) {
                val lineEnd = source.indexOf('\n', index + 2)
                index = if (lineEnd < 0) source.length else lineEnd
            } else if (source.startsWith("/*", index)) {
                val commentEnd = source.indexOf("*/", index + 2)
                index = if (commentEnd < 0) source.length else commentEnd + 1
            } else if (char == opening) {
                depth++
            } else if (char == closing) {
                depth--
                if (depth == 0) return index
            }
            index++
        }
        return -1
    }

    private fun mergeChartValues(current: List<JsValue>?, incoming: List<JsValue>): List<JsValue> {
        if (current == null) return incoming
        val size = maxOf(current.size, incoming.size)
        return (0 until size).map { index ->
            val old = current.getOrNull(index)
            val new = incoming.getOrNull(index)
            if ((old?.intValue() ?: 0) > 0) old!! else new ?: old ?: JsValue("0", false)
        }
    }

    private fun cleanJsText(value: String): String = visibleText(value)

    fun scriptUrls(html: String, pageUrl: String): List<String> = scriptPattern.findAll(html)
        .mapNotNull { attribute(it.value, "src") }
        .mapNotNull { absoluteUrl(it, pageUrl) }
        .filter { runCatching { URL(it).host == URL(pageUrl).host }.getOrDefault(false) }
        .distinct()
        .toList()

    fun parseChart(chart: IidxChart, source: String): TextageChartData {
        val measureTicks = parseMeasureTicks(source)
        val notes = decodeTextageChart(chart, source, measureTicks)
        val measureLengths = parseMeasureLengths(source, measureTicks, notes)
        val bpmChanges = parseBpmChanges(source, measureTicks)
        val lastBeat = notes.maxOfOrNull { it.beat + it.holdBeats }
        val duration = lastBeat?.let { measureEndAtBeat(it, measureLengths) } ?: 0f
        val fallbackBpm = Regex("[0-9]+(?:\\.[0-9]+)?")
            .find(chart.bpm)?.value?.toFloatOrNull()?.takeIf { it in 20f..400f } ?: 150f
        val initialBpm = bpmChanges.firstOrNull()?.bpm ?: fallbackBpm
        val effectiveBpmChanges = if (bpmChanges.isEmpty()) listOf(BpmChange(0f, initialBpm)) else bpmChanges
        // The catalog's note count includes Textage's special-note accounting.
        // Keep it as the source of truth; the renderer deliberately stores one
        // head note for a long note and does not add a second note for release.
        val notesCount = chart.notes.takeIf { it > 0 } ?: notes.size
        return TextageChartData(
            chart = chart.copy(notes = notesCount),
            notes = notes,
            durationBeats = duration,
            parsed = notes.isNotEmpty(),
            bpm = initialBpm,
            bpmChanges = effectiveBpmChanges,
            measureLengths = measureLengths,
            parserMessage = if (notes.isEmpty()) {
                "已获取 Textage 谱面数据，但没有识别到可绘制的时序数据。"
            } else null,
        )
    }

    /**
     * Textage pages contain one sparse `sp`/`dp` array per difficulty. The
     * entries are not HTML notes: they are compact strings consumed by
     * bms2jsh.js. This is the small, deterministic decoder for that format.
     */
    private fun decodeTextageChart(chart: IidxChart, source: String, measureTicks: Map<Int, Int>): List<ChartNote> {
        val modeBody = textageModeBody(source, chart.mode) ?: return emptyList()
        val variable = if (chart.mode == "DP") "dp" else "sp"
        val branchNames = listOf("k", "a", "l", "g", "kuro")
        val branchStart = branchNames.mapNotNull { name ->
            conditionalMatch(modeBody, name)?.range?.first
        }.minOrNull() ?: modeBody.length
        val baseSource = modeBody.substring(0, branchStart)
        val base = parseSparseAssignment(baseSource, variable, emptyList())
        val selectedBranch = when (chart.difficulty) {
            "A" -> "a"
            "L" -> "a"
            "N" -> "l"
            "B" -> "g"
            "H" -> "k"
            else -> "k"
        }?.let { conditionalBody(modeBody, it) }
        val activeSource = selectedBranch ?: baseSource
        val measures = parseSparseAssignment(activeSource, variable, base)

        val notes = ArrayList<ChartNote>()
        val arrays = if (chart.mode == "DP") listOf("sp" to 0, "dp" to 8) else listOf("sp" to 0)
        for ((arrayName, laneOffset) in arrays) {
            val sideMeasures = if (arrayName == variable) measures else {
                val sideBase = parseSparseAssignment(baseSource, arrayName, emptyList())
                selectedBranch?.let { parseSparseAssignment(it, arrayName, sideBase) } ?: sideBase
            }
            val chargeArray = if (arrayName == "sp") "c1" else "c2"
            val chargeBase = parseChargeAssignments(baseSource, emptyMap())
            val chargeMeasures = parseChargeAssignments(activeSource, chargeBase)
            for ((measureIndex, encoded) in sideMeasures.withIndex()) {
                if (measureIndex <= 0 || encoded.isNullOrBlank()) continue
                val ticks = measureTicks[measureIndex] ?: TEXTAGE_BAR_TICKS
                notes += decodeTextageMeasure(
                    encoded = encoded,
                    measureStartBeat = measureStartBeat(measureIndex, measureTicks),
                    measureBeatLength = ticks / TEXTAGE_QUARTER_TICKS,
                    measureTicks = ticks,
                    laneOffset = laneOffset,
                )
            }
            notes += decodeChargeNotes(chargeMeasures[chargeArray].orEmpty(), laneOffset, measureTicks)
        }
        if (notes.isEmpty()) return emptyList()
        return notes
            .distinctBy { Triple(it.beat, it.lane, it.holdBeats) }
            .sortedWith(compareBy<ChartNote> { it.beat }.thenBy { it.lane })
    }

    private fun parseMeasureTicks(source: String): Map<Int, Int> = buildMap {
        Regex("ln\\s*\\[\\s*(\\d+)\\s*]\\s*=\\s*(\\d+)")
            .findAll(source)
            .forEach { match ->
                val measure = match.groupValues[1].toIntOrNull() ?: return@forEach
                val ticks = match.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: return@forEach
                put(measure, ticks)
            }
        Regex("""for\s*\(\s*([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*(\d+)\s*;\s*\1\s*<=\s*(\d+)\s*;\s*\1\+\+\s*\)\s*ln\s*\[\s*\1\s*]\s*=\s*(\d+)""")
            .findAll(source)
            .forEach { match ->
                val start = match.groupValues[2].toIntOrNull() ?: return@forEach
                val end = match.groupValues[3].toIntOrNull() ?: return@forEach
                val ticks = match.groupValues[4].toIntOrNull()?.takeIf { it > 0 } ?: return@forEach
                for (measure in start..end) put(measure, ticks)
            }
    }

    private fun parseMeasureLengths(
        source: String,
        measureTicks: Map<Int, Int>,
        notes: List<ChartNote>,
    ): List<Float> {
        val declared = Regex("measure\\s*=\\s*(\\d+)")
            .find(source)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val lastNoteMeasure = notes.maxOfOrNull { kotlin.math.floor(it.beat / 4f).toInt() + 1 } ?: 0
        val count = maxOf(declared, measureTicks.keys.maxOrNull() ?: 0, lastNoteMeasure)
        if (count <= 0) return emptyList()
        return (1..count).map { (measureTicks[it] ?: TEXTAGE_BAR_TICKS) / TEXTAGE_QUARTER_TICKS }
    }

    private fun parseBpmChanges(source: String, measureTicks: Map<Int, Int>): List<BpmChange> {
        val changes = ArrayList<BpmChange>()
        val entryPattern = Regex("tc\\s*\\[\\s*(\\d+)\\s*]\\s*=\\s*\\[([^]]*)]")
        val valuePattern = Regex("[\\\"']([^\\\"']*)[\\\"']")
        entryPattern.findAll(source).forEach { entry ->
            val measure = entry.groupValues[1].toIntOrNull() ?: return@forEach
            val startBeat = measureStartBeat(measure, measureTicks)
            valuePattern.findAll(entry.groupValues[2]).forEach { valueMatch ->
                val encoded = valueMatch.groupValues[1]
                if (encoded.length < 3) return@forEach
                val bpm = encoded.take(3).trim().toFloatOrNull()?.takeIf { it in 1f..1000f } ?: return@forEach
                val position = encoded.drop(3).trim().toIntOrNull() ?: 0
                val beat = startBeat + position / TEXTAGE_POSITION_TICKS
                changes += BpmChange(beat, bpm)
            }
        }
        return changes
            .sortedBy { it.beat }
            .distinctBy { it.beat }
    }

    private fun measureStartBeat(measureIndex: Int, measureTicks: Map<Int, Int>): Float {
        if (measureIndex <= 1) return 0f
        var beat = 0f
        for (index in 1 until measureIndex) {
            beat += (measureTicks[index] ?: TEXTAGE_BAR_TICKS) / TEXTAGE_QUARTER_TICKS
        }
        return beat
    }

    private fun measureEndAtBeat(beat: Float, measureLengths: List<Float>): Float {
        if (measureLengths.isEmpty()) return (kotlin.math.floor(beat / 4f).toInt() + 1) * 4f
        var start = 0f
        measureLengths.forEach { length ->
            if (beat < start + length) return start + length
            start += length
        }
        return start
    }

    private fun textageModeBody(source: String, mode: String): String? {
        val match = Regex("if\\s*\\(\\s*k\\s*\\)\\s*\\{").find(source) ?: return null
        val ifOpen = source.indexOf('{', match.range.first)
        val ifClose = balancedEnd(source, ifOpen, '{', '}')
        if (ifOpen < 0 || ifClose <= ifOpen) return null
        if (mode != "DP") return source.substring(ifOpen + 1, ifClose)
        val elseMatch = Regex("\\}\\s*else\\s*\\{").find(source, ifClose) ?: return null
        val elseOpen = source.indexOf('{', elseMatch.range.first)
        val elseClose = balancedEnd(source, elseOpen, '{', '}')
        if (elseOpen < 0 || elseClose <= elseOpen) return null
        return source.substring(elseOpen + 1, elseClose)
    }

    private fun conditionalMatch(source: String, name: String): MatchResult? =
        Regex("if\\s*\\(\\s*${Regex.escape(name)}\\s*\\)\\s*\\{").find(source)

    private fun conditionalBody(source: String, name: String): String? {
        val match = conditionalMatch(source, name) ?: return null
        val open = source.indexOf('{', match.range.first)
        val close = balancedEnd(source, open, '{', '}')
        if (open < 0 || close <= open) return null
        return source.substring(open + 1, close)
    }

    private fun parseSparseAssignment(
        source: String,
        variable: String,
        references: List<String?>,
    ): List<String?> {
        val result = references.toMutableList()
        val marker = Regex("\\b${Regex.escape(variable)}\\s*=\\s*\\[").find(source)
        if (marker != null) {
            val open = source.indexOf('[', marker.range.first)
            val close = balancedEnd(source, open, '[', ']')
            if (open >= 0 && close > open) {
                val parsed = parseSparseValues(source.substring(open + 1, close), references)
                result.clear()
                result.addAll(parsed)
            }
        }
        val assignment = Regex("\\b${Regex.escape(variable)}\\s*\\[\\s*(\\d+)\\s*]\\s*=\\s*([^;\\n]+)")
        assignment.findAll(source).forEach { match ->
            val index = match.groupValues[1].toIntOrNull() ?: return@forEach
            val value = parseSparseValue(match.groupValues[2], result)
            while (result.size <= index) result += null
            result[index] = value
        }
        return result
    }

    private fun parseSparseValues(body: String, references: List<String?>): List<String?> {
        val result = ArrayList<String?>()
        splitJsValues(body).forEach { token -> result += parseSparseValue(token, references) }
        return result
    }

    private fun parseSparseValue(token: String, references: List<String?>): String? {
        val trimmed = token.trim().replace(Regex("(?s)/\\*.*?\\*/"), "").trim()
        if (trimmed.isBlank()) return null
        if (trimmed.firstOrNull() == '\'' || trimmed.firstOrNull() == '\"') return readJsString(trimmed, 0).first
        val reference = Regex("^[A-Za-z_$][A-Za-z0-9_$]*\\s*\\[\\s*(\\d+)\\s*]$").find(trimmed)
        if (reference != null) return references.getOrNull(reference.groupValues[1].toInt())
        return trimmed
    }

    /**
     * Textage stores charge notes separately from the compressed sp/dp data.
     * A statement can also assign one pattern to several c1/c2 slots, e.g.
     * `c2[2]=c2[4]=c1[6]=[[7,0,126]];`.
     */
    private fun parseChargeAssignments(
        source: String,
        references: Map<String, MutableMap<Int, List<ChargeEntry>>>,
    ): Map<String, MutableMap<Int, List<ChargeEntry>>> {
        val result = linkedMapOf(
            "c1" to linkedMapOf<Int, List<ChargeEntry>>(),
            "c2" to linkedMapOf<Int, List<ChargeEntry>>(),
        )
        result["c1"]!!.putAll(references["c1"].orEmpty())
        result["c2"]!!.putAll(references["c2"].orEmpty())

        Regex("\\b(c[12])\\s*=\\s*\\[\\s*]\\s*;")
            .findAll(source)
            .forEach { match -> result[match.groupValues[1]]!!.clear() }

        val statements = Regex("(?s)([^;]+);").findAll("$source;").map { it.groupValues[1].trim() }.toList()
        repeat(3) {
            statements.forEach { statement ->
                val parts = statement.split('=')
                if (parts.size < 2) return@forEach
                val targets = parts.dropLast(1).map { token ->
                    Regex("^(c[12])\\s*\\[\\s*(\\d+)\\s*]$").matchEntire(token.trim())
                }
                if (targets.any { it == null }) return@forEach
                val rhs = parts.last().trim()
                val entries = when {
                    rhs.startsWith("[[") -> parseChargeEntries(rhs)
                    else -> Regex("^(c[12])\\s*\\[\\s*(\\d+)\\s*]$")
                        .matchEntire(rhs)
                        ?.let { result[it.groupValues[1]]!![it.groupValues[2].toInt()] }
                } ?: return@forEach
                targets.forEach { target ->
                    val match = target!!
                    result[match.groupValues[1]]!![match.groupValues[2].toInt()] = entries
                }
            }
        }
        return result
    }

    private fun parseChargeEntries(expression: String): List<ChargeEntry> {
        val open = expression.indexOf('[')
        val close = balancedEnd(expression, open, '[', ']')
        if (open < 0 || close <= open) return emptyList()
        val body = expression.substring(open + 1, close)
        val result = ArrayList<ChargeEntry>()
        var index = 0
        while (index < body.length) {
            index = body.indexOf('[', index).takeIf { it >= 0 } ?: break
            val end = balancedEnd(body, index, '[', ']')
            if (end <= index) break
            val values = Regex("-?\\d+").findAll(body.substring(index + 1, end))
                .mapNotNull { it.value.toIntOrNull() }
                .toList()
            val lane = values.getOrNull(0) ?: 0
            val position = values.getOrNull(1) ?: 0
            val length = values.getOrNull(2) ?: 30
            val type = values.getOrNull(3) ?: 3
            result += ChargeEntry(lane, position, length, type)
            index = end + 1
        }
        return result
    }

    private fun decodeChargeNotes(
        measures: Map<Int, List<ChargeEntry>>,
        laneOffset: Int,
        measureTicks: Map<Int, Int>,
    ): List<ChartNote> {
        if (measures.isEmpty()) return emptyList()
        val result = ArrayList<ChartNote>()
        val open = HashMap<Int, OpenCharge>()
        measures.toSortedMap().forEach { (measureIndex, entries) ->
            entries.forEach { entry ->
                val lane = entry.lane
                val startBeat = chargeBeat(measureIndex, entry.position, measureTicks)
                val endBeat = chargeBeat(measureIndex, entry.position + entry.length, measureTicks)
                val begins = entry.type and 1 != 0
                val ends = entry.type and 2 != 0
                val current = open[lane]

                if (begins) {
                    // A malformed/overlapping source entry should not discard
                    // the previous head; close it at the last known position.
                    if (current != null) {
                        result += chartNoteAtBeat(current.startBeat, laneOffset + lane, current.lastBeat - current.startBeat)
                    }
                    if (ends) {
                        result += chartNoteAtBeat(startBeat, laneOffset + lane, (endBeat - startBeat).coerceAtLeast(0f))
                    } else {
                        open[lane] = OpenCharge(startBeat, endBeat)
                    }
                } else if (current != null) {
                    current.lastBeat = maxOf(current.lastBeat, endBeat)
                    if (ends) {
                        result += chartNoteAtBeat(current.startBeat, laneOffset + lane, (endBeat - current.startBeat).coerceAtLeast(0f))
                        open.remove(lane)
                    }
                } else if (ends) {
                    // If the fetched page starts after the beginning of a
                    // continued charge note, retain the visible tail rather
                    // than silently dropping the object.
                    result += chartNoteAtBeat(startBeat, laneOffset + lane, (endBeat - startBeat).coerceAtLeast(0f))
                }
            }
        }
        open.forEach { (lane, charge) ->
            result += chartNoteAtBeat(charge.startBeat, laneOffset + lane, (charge.lastBeat - charge.startBeat).coerceAtLeast(0f))
        }
        return result
    }

    private fun chargeBeat(measureIndex: Int, position: Int, measureTicks: Map<Int, Int>): Float =
        measureStartBeat(measureIndex, measureTicks) + position / 32f

    private fun chartNoteAtBeat(beat: Float, lane: Int, holdBeats: Float): ChartNote = ChartNote(
        beat = beat,
        lane = lane,
        holdBeats = holdBeats,
    )

    private fun splitJsValues(source: String): List<String> {
        val result = ArrayList<String>()
        var start = 0
        var quote: Char? = null
        var escaped = false
        source.forEachIndexed { index, char ->
            if (quote != null) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == quote) quote = null
            } else if (char == '\'' || char == '\"') quote = char
            else if (char == ',') {
                result += source.substring(start, index)
                start = index + 1
            }
        }
        result += source.substring(start)
        return result
    }

    private fun decodeTextageMeasure(
        encoded: String,
        measureStartBeat: Float,
        measureBeatLength: Float,
        measureTicks: Int,
        laneOffset: Int,
    ): List<ChartNote> {
        val result = ArrayList<ChartNote>()
        val normalized = encoded.trim()
        if (normalized.isBlank()) return result
        if (!normalized.startsWith('#')) {
            val cursorStart: Int
            val length: Int
            if (normalized.startsWith('x') && normalized.length >= 4) {
                length = normalized.substring(1, 4).toIntOrNull(16) ?: return result
                cursorStart = 4
            } else {
                length = normalized.length
                cursorStart = 0
            }
            if (length <= 0) return result
            var cursor = cursorStart
            var offset = 0
            while (cursor + 1 < normalized.length) {
                while (normalized.getOrNull(cursor) == '@') {
                    val jump = normalized.substring(cursor + 1, (cursor + 3).coerceAtMost(normalized.length))
                        .toIntOrNull(16) ?: 0
                    offset += jump * 2
                    cursor += 3
                }
                if (cursor + 1 >= normalized.length) break
                val value = normalized.substring(cursor, cursor + 2).toIntOrNull(16) ?: 0
                for (lane in 0..7) {
                    if ((value and (1 shl lane)) != 0) {
                        result += chartNote(
                            measureStartBeat,
                            measureBeatLength,
                            offset.toFloat() / (length * 2f),
                            laneOffset + lane,
                        )
                    }
                }
                cursor += 2
                offset += 2
            }
            return result
        }

        val base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        var cursor = 1
        var compression = 0
        while (cursor < normalized.length) {
            val opcode = normalized[cursor]
            var startOffset = 0
            var step = 0
            var type = 2
            var encodedValue = ""
            var continueAfterDash = false
            when (opcode) {
                'C', 'c', 'R', 'r', 'P', 'p' -> {
                    startOffset = when (opcode) { 'C' -> 0; 'c' -> 96; 'R' -> 0; 'r' -> 48; 'P' -> 0; else -> 24 }
                    step = when (opcode) { 'C', 'c' -> 192; 'R', 'r' -> 96; else -> 48 }
                    type = 0
                    if (compression == 0 && cursor + 1 < normalized.length) {
                        cursor++
                        encodedValue = normalized[cursor].toString()
                    }
                    cursor++
                }
                'B', 'b', 'Q', 'q', 'O', 'o', 'X', 'x', 'Z', 'S', 's', 'T', 't', 'U' -> {
                    val definition = when (opcode) {
                        'B' -> Triple(0, 192, 0); 'b' -> Triple(96, 192, 0)
                        'Q' -> Triple(0, 96, 0); 'q' -> Triple(48, 96, 0)
                        'O' -> Triple(0, 48, 0); 'o' -> Triple(24, 48, 0)
                        'X' -> Triple(0, 24, 0); 'x' -> Triple(12, 24, 0)
                        'Z' -> Triple(0, 12, 0); 'S' -> Triple(0, 64, 0)
                        's' -> Triple(32, 64, 0); 'T' -> Triple(0, 32, 0)
                        't' -> Triple(16, 32, 0); else -> Triple(0, 16, 0)
                    }
                    startOffset = definition.first
                    step = definition.second
                    type = 1
                    val measureValue = if (compression == 0) measureTicks / 2 else measureTicks / 6
                    val count = kotlin.math.ceil(measureValue.toDouble() / step).toInt() + 1
                    val end = (cursor + count).coerceAtMost(normalized.length)
                    encodedValue = normalized.substring((cursor + 1).coerceAtMost(end), end)
                    cursor += count
                }
                in '1'..'7' -> {
                    encodedValue = normalized.substring(cursor, (cursor + 3).coerceAtMost(normalized.length))
                    type = 2
                    cursor += 3
                }
                '8', '9' -> {
                    val builder = StringBuilder()
                    if (opcode == '9' && cursor + 3 < normalized.length) {
                        builder.append('1')
                        builder.append(normalized.substring(cursor + 2, cursor + 4))
                    }
                    val mask = normalized.getOrNull(cursor + 1)?.let(base64::indexOf) ?: 0
                    for (bit in 0..5) {
                        if ((mask and (1 shl bit)) != 0 && cursor + 3 < normalized.length) {
                            builder.append(bit + 2)
                            builder.append(normalized.substring(cursor + 2, cursor + 4))
                        }
                    }
                    encodedValue = builder.toString()
                    type = 2
                    cursor += 4
                }
                '-' -> {
                    compression = 1
                    cursor++
                    continueAfterDash = true
                }
                '_' -> {
                    encodedValue = if (cursor == normalized.lastIndex) "AA" else normalized.substring(cursor + 1)
                    compression = 2
                    type = 2
                }
                else -> return result
            }
            if (continueAfterDash) continue

            if (type == 0) {
                val repeated = if (compression == 0) encodedValue else "1"
                for (position in startOffset until measureTicks step step) {
                    val lane = repeated.firstOrNull()?.digitToIntOrNull() ?: 0
                    if (lane != 0) result += chartNote(measureStartBeat, measureBeatLength, position.toFloat() / measureTicks, laneOffset + lane)
                }
            } else if (type == 1) {
                val decoded = StringBuilder()
                if (compression == 0) {
                    encodedValue.forEach { char ->
                        val value = base64.indexOf(char).coerceAtLeast(0)
                        decoded.append(value / 8)
                        decoded.append(value % 8)
                    }
                } else {
                    encodedValue.forEach { char ->
                        val value = base64.indexOf(char).coerceAtLeast(0)
                        for (bit in 5 downTo 0) decoded.append(if ((value and (1 shl bit)) != 0) '1' else '0')
                    }
                }
                var valueIndex = 0
                for (position in startOffset until measureTicks step step) {
                    val lane = decoded.getOrNull(valueIndex)?.digitToIntOrNull() ?: 0
                    if (lane != 0) result += chartNote(measureStartBeat, measureBeatLength, position.toFloat() / measureTicks, laneOffset + lane)
                    valueIndex++
                }
            } else {
                var valueIndex = 0
                while (valueIndex + 1 < encodedValue.length) {
                    val lane = if (compression == 0) encodedValue[valueIndex++].digitToIntOrNull() ?: 0 else 0
                    val first = base64.indexOf(encodedValue.getOrNull(valueIndex++) ?: 'A')
                    val second = base64.indexOf(encodedValue.getOrNull(valueIndex++) ?: 'A')
                    if (first >= 0 && second >= 0) {
                        result += chartNote(
                            measureStartBeat,
                            measureBeatLength,
                            (first * 64 + second).toFloat() / measureTicks,
                            laneOffset + lane,
                        )
                    }
                }
            }
            if (compression == 2) break
        }
        return result
    }

    private fun chartNote(measureStartBeat: Float, measureBeatLength: Float, fraction: Float, lane: Int): ChartNote = ChartNote(
        beat = measureStartBeat + fraction.coerceIn(0f, 1f) * measureBeatLength,
        lane = lane,
    )

    private fun attribute(html: String, name: String): String? {
        val quoted = Regex("(?is)\\b${Regex.escape(name)}\\s*=\\s*(['\\\"])(.*?)\\1").find(html)?.groupValues?.get(2)
        if (quoted != null) return decodeHtml(quoted)
        return Regex("(?is)\\b${Regex.escape(name)}\\s*=\\s*([^\\s>]+)").find(html)?.groupValues?.get(1)?.let(::decodeHtml)
    }

    private fun visibleText(value: String): String = decodeHtml(tagPattern.replace(value, " "))
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun decodeHtml(value: String): String = value
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
            match.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: match.value
        }
        .replace(Regex("&#(\\d+);")) { match ->
            match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
        }

    private fun absoluteUrl(value: String, pageUrl: String? = null, catalogBase: String? = null): String? {
        val base = pageUrl ?: catalogBase ?: return null
        return runCatching { URL(URL(base), decodeHtml(value)).toString() }.getOrNull()
    }

    private fun slug(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9\\u3040-\\u30ff\\u3400-\\u9fff]+"), "-")
        .trim('-')
        .ifBlank { "song" }

}
