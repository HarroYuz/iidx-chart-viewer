package com.harroyuz.iidxchartviewer.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

internal const val BROWSE_DURATION_MS = 240

@OptIn(ExperimentalSharedTransitionApi::class)
internal data class BrowseMotion(
    val shared: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope?,
    val betweenDetails: Boolean,
    val callerVisible: Boolean = true,
    val movingSongKey: String? = null,
)

internal val LocalBrowseMotion = staticCompositionLocalOf<BrowseMotion?> { null }

/** Identical content uses a single overlay instead of two overlapping fades. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.browseSharedBounds(
    key: String?,
    stable: Boolean = false,
    stableInDetails: Boolean = false,
    overlayZ: Float? = null,
): Modifier {
    val motion = LocalBrowseMotion.current ?: return this
    if (key == null) return this
    return with(motion.shared) {
        val state = rememberSharedContentState(key)
        if (motion.visibility == null) {
            sharedElementWithCallerManagedVisibility(
                sharedContentState = state,
                visible = motion.callerVisible,
                boundsTransform = { _, _ -> tween(BROWSE_DURATION_MS, easing = FastOutSlowInEasing) },
                zIndexInOverlay = overlayZ ?: if (stable) 2f else 0f,
            )
        } else if (stable || (stableInDetails && motion.betweenDetails)) {
            sharedElement(
                sharedContentState = state,
                animatedVisibilityScope = motion.visibility,
                boundsTransform = { _, _ -> tween(BROWSE_DURATION_MS, easing = FastOutSlowInEasing) },
                zIndexInOverlay = overlayZ ?: 2f,
            )
        } else {
            sharedBounds(
                sharedContentState = state,
                animatedVisibilityScope = motion.visibility,
                boundsTransform = { _, _ -> tween(BROWSE_DURATION_MS, easing = FastOutSlowInEasing) },
                enter = fadeIn(tween(147, delayMillis = 60)),
                exit = fadeOut(tween(120)),
                resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
                zIndexInOverlay = overlayZ ?: 0f,
            )
        }
    }
}

@Composable
internal fun Modifier.browseSongSurface(key: String): Modifier =
    if (LocalBrowseMotion.current?.betweenDetails == true) this else browseSharedBounds(key)

/** Only new player content moves; the header stays outside this animation. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.browseReveal(fromBottom: Boolean = false): Modifier {
    val motion = LocalBrowseMotion.current ?: return this
    val visibility = motion.visibility ?: return this
    val layer = if (fromBottom) {
        with(motion.shared) { renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f) }
    } else this
    return with(visibility) {
        layer.animateEnterExit(
            enter = if (fromBottom) {
                slideInVertically(tween(BROWSE_DURATION_MS, easing = FastOutSlowInEasing)) { it }
            } else fadeIn(tween(147, delayMillis = 60)),
            exit = if (fromBottom) {
                slideOutVertically(tween(BROWSE_DURATION_MS, easing = FastOutSlowInEasing)) { it }
            } else fadeOut(tween(120)),
            label = if (fromBottom) "player-reveal" else "notes-reveal",
        )
    }
}

/** Measure fixed controls at their final size throughout a shared transition. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.keepSharedSize(): Modifier {
    val motion = LocalBrowseMotion.current ?: return this
    return with(motion.shared) { skipToLookaheadSize() }
}
