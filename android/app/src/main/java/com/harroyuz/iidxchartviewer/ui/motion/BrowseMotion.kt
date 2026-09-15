package com.harroyuz.iidxchartviewer.ui.motion

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout

internal const val BROWSE_DURATION_MS = 240

@OptIn(ExperimentalSharedTransitionApi::class)
internal data class BrowseMotion(
    val shared: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope,
    val betweenDetails: Boolean,
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
        if (stable || (stableInDetails && motion.betweenDetails)) {
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
    val visibility = motion.visibility
    if (fromBottom) {
        val offset = visibility.transition.animateFloat(
            transitionSpec = { tween(BROWSE_DURATION_MS, easing = FastOutSlowInEasing) },
            label = "detail-reveal",
        ) { if (it == EnterExitState.Visible) 0f else 1f }
        return with(motion.shared) {
            renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
                .layout { measurable, constraints ->
                    val child = measurable.measure(constraints)
                    layout(child.width, child.height) {
                        if (isLookingAhead) {
                            child.place(0, 0)
                        } else {
                            val top = coordinates?.let { lookaheadScopeCoordinates.localPositionOf(it, Offset.Zero).y } ?: 0f
                            val distance = bottomRevealDistance(lookaheadScopeCoordinates.size.height.toFloat(), top)
                            child.placeWithLayer(0, 0) { translationY = distance * offset.value }
                        }
                    }
                }
        }
    }
    return with(visibility) {
        animateEnterExit(
            enter = fadeIn(tween(147, delayMillis = 60)),
            exit = fadeOut(tween(120)),
            label = "notes-reveal",
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

/** Connect a retained page to the same visibility clock as the transient detail pages. */
internal class RetainedVisibilityScope(
    override val transition: Transition<EnterExitState>,
) : AnimatedVisibilityScope

/** Other catalog cards enter as the selected card contracts; only its shared copy is drawn. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.browseCatalogItem(key: String, aboveSelected: Boolean): Modifier {
    val motion = LocalBrowseMotion.current ?: return this
    if (motion.movingSongKey == null) return this
    val visibility = motion.visibility.transition
    if (motion.movingSongKey == key) {
        return graphicsLayer {
            alpha = if (visibility.isRunning || visibility.currentState != visibility.targetState) 0f else 1f
        }
    }
    val offset = visibility.animateFloat(
        transitionSpec = { tween(BROWSE_DURATION_MS, easing = FastOutSlowInEasing) },
        label = "catalog-neighbors",
    ) { if (it == EnterExitState.Visible) 0f else 1f }
    return with(motion.shared) {
        layout { measurable, constraints ->
            val child = measurable.measure(constraints)
            layout(child.width, child.height) {
                if (isLookingAhead) {
                    child.place(0, 0)
                } else {
                    val top = coordinates?.let { lookaheadScopeCoordinates.localPositionOf(it, Offset.Zero).y } ?: 0f
                    val distance = catalogRevealDistance(lookaheadScopeCoordinates.size.height.toFloat(), top, child.height.toFloat(), aboveSelected)
                    child.placeWithLayer(0, 0) { translationY = distance * offset.value }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.browseCatalogChrome(): Modifier {
    val motion = LocalBrowseMotion.current ?: return this
    val alpha = motion.visibility.transition.animateFloat(
        transitionSpec = { tween(BROWSE_DURATION_MS, easing = FastOutSlowInEasing) },
        label = "catalog-controls",
    ) { if (it == EnterExitState.Visible) 1f else 0f }
    return graphicsLayer { this.alpha = alpha.value }
}
