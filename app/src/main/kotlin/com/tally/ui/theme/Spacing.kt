package com.tally.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale — a 4 dp grid. Composables pull gaps and padding from here
 * (via `TallyTheme.spacing`) so rhythm stays consistent and a single edit
 * re-tunes the whole app.
 *
 * [minTouchTarget] encodes the constitution's 44 dp minimum hit target; every
 * interactive atom sizes against it (an accessibility line item in the DoD).
 */
@Immutable
data class TallySpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val screenPadding: Dp = 16.dp,
    val minTouchTarget: Dp = 44.dp,
)

val LocalTallySpacing = staticCompositionLocalOf { TallySpacing() }
