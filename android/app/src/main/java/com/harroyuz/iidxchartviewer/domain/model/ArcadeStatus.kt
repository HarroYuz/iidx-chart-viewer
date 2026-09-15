package com.harroyuz.iidxchartviewer.domain.model

/** Textage actbl[tag][0] bit 0 means currently included in arcade IIDX. */
enum class ArcadeStatus(val priority: Int) {
    UNKNOWN(1), CURRENT(2), DELETED(0), CONSUMER_ONLY(0);

    val isUnavailable: Boolean get() = this == DELETED || this == CONSUMER_ONLY

    companion object {
        fun fromStored(value: String?): ArcadeStatus = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}
