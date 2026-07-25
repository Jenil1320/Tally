package com.tally.ui.components.chart

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tally.ui.theme.TallyTheme

/**
 * A limit bar: spend so far ([fillFraction]) drawn over a track, with a tick at
 * [paceFraction] marking how far through the month it is.
 *
 * The tick is the point of the whole component — a fill short of it means
 * spending is behind the month, level with it means on pace, past it means the
 * limit arrives early. Hand-rolled Canvas (no chart library, per the
 * constitution); both fractions are clamped to `0f..1f`.
 */
@Composable
fun TLimitBar(
    fillFraction: Float,
    paceFraction: Float,
    modifier: Modifier = Modifier,
    fillColor: Color = TallyTheme.colors.accent,
    trackColor: Color = TallyTheme.colors.bg3,
    tickColor: Color = TallyTheme.colors.textMid,
) {
    Canvas(modifier = modifier) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = trackColor, size = size, cornerRadius = radius)

        val fill = fillFraction.coerceIn(0f, 1f)
        if (fill > 0f) {
            drawRoundRect(
                color = fillColor,
                size = Size((size.width * fill).coerceAtLeast(size.height), size.height),
                cornerRadius = radius,
            )
        }

        val tickWidth = 2.dp.toPx()
        val x = (size.width * paceFraction.coerceIn(0f, 1f) - tickWidth / 2f).coerceIn(0f, size.width - tickWidth)
        drawRect(
            color = tickColor,
            topLeft = Offset(x, -size.height * 0.35f),
            size = Size(tickWidth, size.height * 1.7f),
        )
    }
}
