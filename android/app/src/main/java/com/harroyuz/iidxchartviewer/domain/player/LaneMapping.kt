package com.harroyuz.iidxchartviewer.domain.player

internal fun dpDisplayLane(rawLane: Int, destinationKeyLane: Int): Int = when {
    rawLane == 8 -> 15
    rawLane in 9..15 -> 8 + destinationKeyLane - 1
    else -> rawLane
}

internal fun dpOptionSourceLane(lane: Int, flip: Boolean): Int =
    if (flip && lane in 0..15) (lane + 8) % 16 else lane
