package com.harroyuz.iidxchartviewer.data.text

import org.jsoup.parser.Parser

/** Decode once, preserving literal markup and supporting named entities and Unicode code points. */
internal fun decodeHtmlEntities(value: String): String =
    if ('&' !in value) value else Parser.unescapeEntities(value, false).replace('\u00a0', ' ')
