package com.harroyuz.iidxchartviewer.domain.catalog

/** A non-empty selection of the two favorite states. */
internal enum class FavoriteFilter(val label: String) {
    ALL("全部"), FAVORITED("已收藏"), NOT_FAVORITED("未收藏");

    fun matches(isFavorite: Boolean): Boolean = when (this) {
        ALL -> true
        FAVORITED -> isFavorite
        NOT_FAVORITED -> !isFavorite
    }

    fun toggle(isFavorite: Boolean): FavoriteFilter = when {
        this == ALL -> if (isFavorite) NOT_FAVORITED else FAVORITED
        matches(isFavorite) -> this
        else -> ALL
    }
}
