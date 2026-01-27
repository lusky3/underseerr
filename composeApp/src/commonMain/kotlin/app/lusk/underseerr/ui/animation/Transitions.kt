package app.lusk.underseerr.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

/**
 * Animation transitions for screen navigation and UI elements.
 * KMP Compatible.
 */

/**
 * Standard animation duration in milliseconds.
 */
object AnimationDuration {
    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 500
}

/**
 * Enter transition for screens sliding in from the right.
 */
fun slideInFromRight(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = LinearOutSlowInEasing
        )
    )
}

/**
 * Exit transition for screens sliding out to the left.
 */
fun slideOutToLeft(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = FastOutSlowInEasing
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = LinearOutSlowInEasing
        )
    )
}

/**
 * Enter transition for screens sliding in from the left.
 */
fun slideInFromLeft(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = LinearOutSlowInEasing
        )
    )
}

/**
 * Exit transition for screens sliding out to the right.
 */
fun slideOutToRight(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = FastOutSlowInEasing
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = LinearOutSlowInEasing
        )
    )
}

/**
 * Enter transition for screens sliding in from the bottom.
 */
fun slideInFromBottom(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight },
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = AnimationDuration.SHORT,
            easing = LinearOutSlowInEasing
        )
    )
}

/**
 * Exit transition for screens sliding out to the bottom.
 */
fun slideOutToBottom(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight },
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = FastOutSlowInEasing
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = AnimationDuration.SHORT,
            easing = LinearOutSlowInEasing
        )
    )
}

/**
 * Enter transition with fade and scale effect.
 */
fun fadeInWithScale(): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = LinearOutSlowInEasing
        )
    ) + scaleIn(
        initialScale = 0.9f,
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = FastOutSlowInEasing
        )
    )
}

/**
 * Exit transition with fade and scale effect.
 */
fun fadeOutWithScale(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = LinearOutSlowInEasing
        )
    ) + scaleOut(
        targetScale = 0.9f,
        animationSpec = tween(
            durationMillis = AnimationDuration.MEDIUM,
            easing = FastOutSlowInEasing
        )
    )
}

/**
 * Simple fade in transition.
 */
fun simpleFadeIn(): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = AnimationDuration.SHORT,
            easing = LinearOutSlowInEasing
        )
    )
}

/**
 * Simple fade out transition.
 */
fun simpleFadeOut(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = AnimationDuration.SHORT,
            easing = LinearOutSlowInEasing
        )
    )
}

/**
 * Navigation transition for forward navigation.
 */
fun <T> AnimatedContentTransitionScope<T>.forwardTransition(): EnterTransition {
    return slideInFromRight()
}

/**
 * Navigation transition for backward navigation.
 */
fun <T> AnimatedContentTransitionScope<T>.backwardTransition(): ExitTransition {
    return slideOutToRight()
}

/**
 * Navigation transition for pop enter (when returning to a screen).
 */
fun <T> AnimatedContentTransitionScope<T>.popEnterTransition(): EnterTransition {
    return slideInFromLeft()
}

/**
 * Navigation transition for pop exit (when leaving a screen).
 */
fun <T> AnimatedContentTransitionScope<T>.popExitTransition(): ExitTransition {
    return slideOutToLeft()
}
