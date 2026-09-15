package com.harroyuz.iidxchartviewer.domain.player

/** Optional disk-cache preparation must never compete for scarce device memory. */
internal fun canPrefetchCharts(lowRamDevice: Boolean, lowMemory: Boolean, memoryClassMb: Int, availableBytes: Long): Boolean =
    !lowRamDevice && !lowMemory && memoryClassMb >= 128 && availableBytes >= 192L * 1024 * 1024

internal const val CHART_PREFETCH_LIMIT = 2
internal const val CHART_PREFETCH_DELAY_MS = 750L
