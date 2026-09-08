package com.harroyuz.iidxchartviewer.domain.player

internal fun dpDisplayLane(rawLane: Int, destinationKeyLane: Int): Int = when {
    rawLane == 8 -> 15
    rawLane in 9..15 -> 8 + destinationKeyLane - 1
    else -> rawLane
}
