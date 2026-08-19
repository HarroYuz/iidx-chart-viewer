package com.harroyuz.iidxchartviewer

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import java.net.URL

/**
 * The chart page is data-driven JavaScript. Rhino is used only as a sandboxed
 * evaluator for the same data scripts that Textage loads in a browser; it is
 * not used to display a page or to run arbitrary application JavaScript.
 */
internal object TextageJsEngine {
    private val scriptBlockPattern = Regex("(?is)<script\\b([^>]*)>(.*?)</script\\s*>")

    fun execute(
        pageUrl: String,
        pageSource: String,
        externalScripts: List<String>,
    ): TextageJsSnapshot? = runCatching {
        val context = Context.enter()
        try {
            // Rhino's bytecode optimizer is not suitable for every Android VM.
            context.setOptimizationLevel(-1)
            context.setInstructionObserverThreshold(100_000)
            val scope = context.initStandardObjects()
            context.evaluateString(scope, bootstrap(pageUrl), "textage-bootstrap.js", 1, null)
            externalScripts.forEachIndexed { index, script ->
                context.evaluateString(scope, script, "textage-external-$index.js", 1, null)
            }
            inlineScripts(pageSource).forEachIndexed { index, script ->
                context.evaluateString(scope, script, "textage-page-$index.js", 1, null)
            }
            context.evaluateString(scope, collectNotesScript(), "textage-collect-notes.js", 1, null)
            snapshot(scope)
        } finally {
            Context.exit()
        }
    }.getOrNull()

    private fun inlineScripts(pageSource: String): List<String> = scriptBlockPattern.findAll(pageSource)
        .filter { match ->
            val attributes = match.groupValues[1]
            !Regex("(?i)\\bsrc\\s*=").containsMatchIn(attributes)
        }
        .map { it.groupValues[2] }
        .filter { it.isNotBlank() }
        .toList()

    private fun snapshot(scope: Scriptable): TextageJsSnapshot {
        return TextageJsSnapshot(
            notes = readInt(ScriptableObject.getProperty(scope, "notes")),
            measure = readInt(ScriptableObject.getProperty(scope, "measure")),
            sp = readSparseArray(ScriptableObject.getProperty(scope, "sp")),
            dp = readSparseArray(ScriptableObject.getProperty(scope, "dp")),
            measureTicks = readIntMap(ScriptableObject.getProperty(scope, "ln")),
            bpmChanges = readStringArrayMap(ScriptableObject.getProperty(scope, "tc")),
            charges1 = readChargeMap(ScriptableObject.getProperty(scope, "c1")),
            charges2 = readChargeMap(ScriptableObject.getProperty(scope, "c2")),
            notesEvents = readNoteEvents(ScriptableObject.getProperty(scope, "__textageEvents")),
        )
    }

    private fun readNoteEvents(value: Any?): List<TextageJsNoteEvent> = readArray(value).mapNotNull { entry ->
        val values = readArray(entry).mapNotNull(::readInt)
        if (values.size < 4) null else TextageJsNoteEvent(
            side = values[0],
            lane = values[1],
            position = values[2],
            length = values[3],
        )
    }

    private fun readSparseArray(value: Any?): List<String?> {
        val array = value as? Scriptable ?: return emptyList()
        val length = readInt(ScriptableObject.getProperty(array, "length")) ?: return emptyList()
        return (0 until length.coerceAtMost(10_000)).map { index ->
            val item = ScriptableObject.getProperty(array, index)
            if (item == null || item is Undefined) null else Context.toString(item)
        }
    }

    private fun readIntMap(value: Any?): Map<Int, Int> {
        val array = value as? Scriptable ?: return emptyMap()
        val length = readInt(ScriptableObject.getProperty(array, "length")) ?: return emptyMap()
        return (0 until length.coerceAtMost(10_000)).mapNotNull { index ->
            readInt(ScriptableObject.getProperty(array, index))?.let { index to it }
        }.toMap()
    }

    private fun readStringArrayMap(value: Any?): Map<Int, List<String>> {
        val array = value as? Scriptable ?: return emptyMap()
        val length = readInt(ScriptableObject.getProperty(array, "length")) ?: return emptyMap()
        return (0 until length.coerceAtMost(10_000)).mapNotNull { index ->
            val values = readArray(ScriptableObject.getProperty(array, index))
                .mapNotNull { item ->
                    if (item == null || item is Undefined) null else Context.toString(item)
                }
            if (values.isEmpty()) null else index to values
        }.toMap()
    }

    private fun readChargeMap(value: Any?): Map<Int, List<List<Int>>> {
        val array = value as? Scriptable ?: return emptyMap()
        val length = readInt(ScriptableObject.getProperty(array, "length")) ?: return emptyMap()
        return (0 until length.coerceAtMost(10_000)).mapNotNull { index ->
            val entries = readArray(ScriptableObject.getProperty(array, index)).mapNotNull { entry ->
                val values = readArray(entry).mapNotNull(::readInt)
                values.takeIf { it.isNotEmpty() }
            }
            if (entries.isEmpty()) null else index to entries
        }.toMap()
    }

    private fun readArray(value: Any?): List<Any?> {
        val array = value as? Scriptable ?: return emptyList()
        val length = readInt(ScriptableObject.getProperty(array, "length")) ?: return emptyList()
        return (0 until length.coerceAtMost(10_000)).map { index ->
            ScriptableObject.getProperty(array, index)
        }
    }

    private fun readInt(value: Any?): Int? {
        if (value == null || value is Undefined) return null
        val number = runCatching { Context.toNumber(value) }.getOrNull() ?: return null
        return number.takeIf { it.isFinite() }?.toInt()
    }

    private fun bootstrap(pageUrl: String): String {
        val url = URL(pageUrl)
        val urlText = jsString(pageUrl)
        val path = jsString(url.path)
        val search = jsString(url.query?.let { "?$it" }.orEmpty())
        return """
            var __output = "";
            var __noop = function() {};
            var document = {
                URL: $urlText,
                referrer: "",
                title: "",
                documentElement: {scrollWidth: 1000, clientHeight: 1000},
                images: [],
                write: function(value) { __output += String(value); },
                querySelector: function() { return {setAttribute: __noop}; },
                getElementById: function() { return {innerHTML: ""}; },
                createElement: function() { return {style: {}, setAttribute: __noop}; }
            };
            var location = {
                href: $urlText,
                pathname: $path,
                search: $search,
                replace: __noop
            };
            var window = {
                document: document,
                location: location,
                pageYOffset: 0,
                open: __noop,
                confirm: function() { return false; }
            };
            var navigator = {};
            var event = {};
            var adsbygoogle = [];
            function scrollTo() {}
            function alert() {}
            function prompt() { return ""; }
            function confirm() { return false; }
            function setTimeout() {}
            function clearTimeout() {}
        """.trimIndent()
    }

    /**
     * Textage's official renderer already contains the complete decoder for
     * compressed note strings. Run its statistics pass after the page data is
     * loaded and capture the same positions it records for the browser view.
     */
    private fun collectNotesScript(): String = """
        var __textageEvents = [];
        var __textageOriginalStatInsert = stat_insert;
        stat_insert = function(side, lane, position, length) {
            __textageEvents.push([side, lane, position, length || 0]);
            return __textageOriginalStatInsert(side, lane, position, length);
        };
        stat_arr = [[[],[],[],[],[],[],[],[]],[[],[],[],[],[],[],[]],[[]]];
        stat_pos = 0;
        stat_on = 1;
        b(0, measure);
        stat_on = 0;
    """.trimIndent()

    private fun jsString(value: String): String = "'" + value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r", "\\r")
        .replace("\n", "\\n") + "'"
}

internal data class TextageJsSnapshot(
    val notes: Int?,
    val measure: Int?,
    val sp: List<String?>,
    val dp: List<String?>,
    val measureTicks: Map<Int, Int>,
    val bpmChanges: Map<Int, List<String>>,
    val charges1: Map<Int, List<List<Int>>>,
    val charges2: Map<Int, List<List<Int>>>,
    val notesEvents: List<TextageJsNoteEvent>,
)

internal data class TextageJsNoteEvent(
    val side: Int,
    val lane: Int,
    val position: Int,
    val length: Int,
)
