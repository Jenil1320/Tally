package com.tally.ui.components.chart

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.tally.ui.theme.TallyTheme

/**
 * A hand-rolled vertical bar chart (Compose Canvas — no chart library). Bars are
 * normalised to the largest value and grow from a baseline; an optional
 * [highlightIndex] paints one bar in the accent (e.g. today, or the peak day).
 * Zero values render as a faint stub so gaps stay legible.
 */
@Composable
fun TBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = TallyTheme.colors.accentDim,
    highlightIndex: Int? = null,
    highlightColor: Color = TallyTheme.colors.accent,
) {
    val stubColor = TallyTheme.colors.bg3
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val maxValue = values.maxOrNull() ?: 0f
        val gap = size.width / (values.size * 4f)
        val barWidth = (size.width - gap * (values.size - 1)) / values.size
        val radius = CornerRadius(barWidth / 3f, barWidth / 3f)
        val stub = 2f
        values.forEachIndexed { index, value ->
            val barHeight = if (maxValue > 0f) (value / maxValue) * size.height else 0f
            val drawHeight = barHeight.coerceAtLeast(stub)
            val x = index * (barWidth + gap)
            drawRoundRect(
                color =
                    if (value <= 0f) {
                        stubColor
                    } else if (index == highlightIndex) {
                        highlightColor
                    } else {
                        barColor
                    },
                topLeft = Offset(x, size.height - drawHeight),
                size = Size(barWidth, drawHeight),
                cornerRadius = radius,
            )
        }
    }
}
