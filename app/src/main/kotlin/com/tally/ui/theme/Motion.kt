package com.tally.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Motion tokens. Tally moves calmly: one easing curve, one default duration, and
 * no springs. If you can hear the spring, it is wrong (design canvas, rule F01).
 *
 * - [standard] (220 ms) is for every entrance, exit, colour change and sheet.
 * - [countUp] (400 ms, linear) is for a figure that changes value.
 */
object TallyMotion {
    const val STANDARD_MS = 220
    const val COUNT_UP_MS = 400
    const val DRAW_IN_MS = 600

    /** Ease-out curve from the design canvas: cubic-bezier(0.2, 0.7, 0.3, 1). */
    val easing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)

    fun <T> standard(): FiniteAnimationSpec<T> = tween(durationMillis = STANDARD_MS, easing = easing)

    fun <T> countUp(): FiniteAnimationSpec<T> = tween(durationMillis = COUNT_UP_MS, easing = LinearEasing)

    fun <T> drawIn(): FiniteAnimationSpec<T> = tween(durationMillis = DRAW_IN_MS, easing = easing)
}

/**
 * True when motion should be skipped: charts appear complete and figures change
 * at once. It follows the system "remove animations" setting, and screenshot
 * tests set it so a golden never captures a half-drawn frame.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Reads the system setting: animations are off when the animator duration scale is zero. */
@Composable
fun rememberSystemReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            .getOrDefault(false)
    }
}
