package com.harroyuz.iidxchartviewer.data.local

import com.harroyuz.iidxchartviewer.data.text.decodeHtmlEntities
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.model.BjmMusic

/** Only for pre-v5 cached catalog text; IDs, URLs, scores and note counts remain unchanged. */
internal fun IidxChart.decodeLegacyTextageEntities(): IidxChart = copy(
    title = decodeHtmlEntities(title),
    subtitle = decodeHtmlEntities(subtitle),
    genre = decodeHtmlEntities(genre),
    composer = decodeHtmlEntities(composer),
    sourceLabel = decodeHtmlEntities(sourceLabel),
    bpm = decodeHtmlEntities(bpm),
    version = decodeHtmlEntities(version),
)

/** Only for music cached before entity decoding was added to BjmMusicProto. */
internal fun BjmMusic.decodeLegacyBjmEntities(): BjmMusic = copy(
    title = decodeHtmlEntities(title),
    plainTitle = decodeHtmlEntities(plainTitle),
    genre = decodeHtmlEntities(genre),
    artist = decodeHtmlEntities(artist),
)
