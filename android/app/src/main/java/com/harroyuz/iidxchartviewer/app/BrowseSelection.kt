package com.harroyuz.iidxchartviewer.app

import com.harroyuz.iidxchartviewer.domain.model.IidxChart

/** The return destination follows the player's current style without adding a page to quick browsing. */
internal data class BrowseSelection(
    val song: IidxChart? = null,
    val chart: IidxChart? = null,
) {
    fun openChart(next: IidxChart): BrowseSelection =
        copy(song = if (song == null) null else next, chart = next)

    fun back(): BrowseSelection =
        if (chart != null) copy(chart = null) else BrowseSelection()
}
