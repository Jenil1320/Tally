package com.tally.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Corner-radius tokens. Tally's surfaces are softly rounded and consistent;
 * [card] is the workhorse (transaction rows, panels), [sheet] is the top radius
 * for the Add bottom sheet, [pill] is fully rounded for chips and the FAB.
 */
@Immutable
data class TallyShapes(
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
    val card: RoundedCornerShape = RoundedCornerShape(20.dp),
    val large: RoundedCornerShape = RoundedCornerShape(20.dp),
    val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
)

val LocalTallyShapes = staticCompositionLocalOf { TallyShapes() }
