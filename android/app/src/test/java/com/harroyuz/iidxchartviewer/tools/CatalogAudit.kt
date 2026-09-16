package com.harroyuz.iidxchartviewer.tools

import com.harroyuz.iidxchartviewer.data.remote.bjm.BjmMusicProto
import com.harroyuz.iidxchartviewer.data.remote.textage.TextageParser
import com.harroyuz.iidxchartviewer.domain.catalog.buildSongGroups
import com.harroyuz.iidxchartviewer.domain.catalog.chartSongKey
import com.harroyuz.iidxchartviewer.domain.model.IidxAppState
import com.harroyuz.iidxchartviewer.domain.score.buildBjmIndex
import java.io.File
import kotlinx.coroutines.runBlocking

/** Explicit offline audit using the production parser and matching code; never reads user data. */
object CatalogAudit {
    @JvmStatic fun main(args: Array<String>) = runBlocking {
        require(args.size == 3) { "Usage: CatalogAudit <textage UTF-8 source> <BJM mdb bin> <output directory>" }
        val charts = TextageParser.parseCatalog(File(args[0]).readText()) { _, _, _ -> }
        val music = BjmMusicProto.decode(File(args[1]).readBytes())
        require(charts.size > 7000 && music.size > 1000) { "Incomplete input catalog" }
        val groups = buildSongGroups(charts)
        val state = IidxAppState(charts = charts, songGroups = groups, bjmMusic = music)
        val index = buildBjmIndex(state, 0, 0, 0)
        val byId = charts.associateBy { it.id }
        val directory = File(args[2]).apply { mkdirs() }
        fun table(name: String, header: List<String>, rows: List<List<Any?>>) {
            File(directory, name).bufferedWriter().use { writer ->
                (listOf(header) + rows).forEach { row ->
                    writer.appendLine(row.joinToString("\t") { value ->
                        value?.toString().orEmpty().replace("\t", " ").replace("\n", " ").replace("\r", " ")
                    })
                }
            }
        }
        table("songs.tsv", listOf("keys", "title", "subtitle", "genre", "composer", "version", "status", "bjm_id", "urls", "charts"), groups.map { group ->
            val members = group.chartIds.mapNotNull(byId::get)
            listOf(
                members.map(::chartSongKey).distinct().sorted().joinToString(";"),
                group.title, group.subtitle, group.genre, group.composer, group.version,
                members.map { it.arcadeStatus.name }.distinct().sorted().joinToString(";"),
                index.songMusicIds[group.key],
                members.mapNotNull { it.textageUrl?.substringBefore('?') }.distinct().sorted().joinToString(";"),
                members.joinToString(";") { "${it.mode}${it.difficulty}:${it.level}:${it.notes}" },
            )
        })
        table("music.tsv", listOf("id", "title", "plain_title", "genre", "artist", "version", "levels"), music.map {
            listOf(it.musicId, it.title, it.plainTitle, it.genre, it.artist, it.version, it.levels.joinToString(";"))
        })
        println("charts=${charts.size}, songs=${groups.size}, BJM=${music.size}, unmatched=${groups.count { it.key !in index.songMusicIds }}")
    }
}
