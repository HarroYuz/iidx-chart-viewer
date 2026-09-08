package com.harroyuz.iidxchartviewer.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** One owner for both levels, so nested surfaces and metadata follow the same clock. */
@OptIn(ExperimentalSharedTransitionApi::class)
internal data class BrowseMotion(
    val shared: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope,
)

internal val LocalBrowseMotion = staticCompositionLocalOf<BrowseMotion?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.browseSharedBounds(key: String?): Modifier {
    val motion = LocalBrowseMotion.current ?: return this
    if (key == null) return this
    return with(motion.shared) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = motion.visibility,
            boundsTransform = { _, _ -> tween(360, easing = FastOutSlowInEasing) },
            enter = fadeIn(tween(220, delayMillis = 90)),
            exit = fadeOut(tween(180)),
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
        )
    }
}
