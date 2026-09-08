package com.harroyuz.iidxchartviewer.data.local

import com.harroyuz.iidxchartviewer.domain.model.BjmMusic
import com.harroyuz.iidxchartviewer.domain.model.BjmScore
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.model.IidxSongGroup
import org.json.JSONArray
import org.json.JSONObject

internal fun IidxChart.toJson() = JSONObject().apply {
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
    put("source_label", sourceLabel)
    put("score", score ?: JSONObject.NULL)
    put("confirmed", confirmed)
    put("textage_url", textageUrl ?: JSONObject.NULL)
}

internal fun JSONObject.toChart(): IidxChart {
    val version = optString("version", "Textage")
    return IidxChart(
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
    version = version,
    sourceLabel = optString("source_label"),
    score = if (isNull("score")) null else optInt("score"),
    confirmed = optBoolean("confirmed"),
    textageUrl = if (isNull("textage_url")) null else optString("textage_url")
        .takeIf { it.isNotBlank() }
        ?.normalizeTextageUrl(version),
    )
}

internal fun IidxSongGroup.toJson() = JSONObject().apply {
    put("key", key)
    put("title", title)
    put("subtitle", subtitle)
    put("genre", genre)
    put("composer", composer)
    put("version", version)
    put("source_label", sourceLabel)
    put("chart_ids", JSONArray(chartIds))
}

internal fun JSONObject.toSongGroup(): IidxSongGroup = IidxSongGroup(
    key = optString("key"),
    title = optString("title"),
    subtitle = optString("subtitle"),
    genre = optString("genre"),
    composer = optString("composer"),
    version = optString("version"),
    sourceLabel = optString("source_label"),
    chartIds = optJSONArray("chart_ids")?.let { array ->
        buildList {
            for (index in 0 until array.length()) add(array.optString(index))
        }
    } ?: emptyList(),
)

internal fun String.normalizeTextageUrl(version: String): String =
    if (version.equals("substream", ignoreCase = true)) {
        replace(Regex("/score/[^/]+/"), "/score/s/")
    } else {
        this
    }

internal fun BjmScore.toJson() = JSONObject().apply {
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

internal fun JSONObject.toScore() = BjmScore(
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

internal fun BjmMusic.toJson() = JSONObject().apply {
    put("music_id", musicId)
    put("title", title)
    put("plain_title", plainTitle)
    put("genre", genre)
    put("artist", artist)
    put("version", version)
    put("levels", JSONArray(levels))
}

internal fun JSONObject.toBjmMusic() = BjmMusic(
    musicId = optInt("music_id"),
    title = optString("title"),
    plainTitle = optString("plain_title"),
    genre = optString("genre"),
    artist = optString("artist"),
    version = optInt("version"),
    levels = optJSONArray("levels")?.let { array ->
        buildList {
            for (index in 0 until array.length()) add(array.optString(index))
        }
    } ?: emptyList(),
)
