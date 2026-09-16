package com.harroyuz.iidxchartviewer.domain.catalog

import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.domain.model.IidxChart

internal fun IidxChart.matchesCatalogFilters(
    version: String?,
    level: Int?,
    hideDeleted: Boolean,
    hideConsumer: Boolean,
): Boolean =
    (version == null || this.version == version) &&
        (level == null || this.level == level) &&
        (!hideDeleted || arcadeStatus != ArcadeStatus.DELETED) &&
        (!hideConsumer || arcadeStatus != ArcadeStatus.CONSUMER_ONLY)
