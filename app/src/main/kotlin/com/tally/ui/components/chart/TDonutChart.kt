package com.tally.ui.components.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tally.ui.theme.TallyTheme
import kotlin.math.min

/** A single ring slice: a [fraction] (0..1) of the circle in [color]. */
data class DonutSlice(
    val fraction: Float,
    val color: Color,
)

/**
 * A hand-rolled donut chart (Compose Canvas — no chart libraries, per the
 * constitution). Slices start at 12 o'clock and run clockwise with a small gap
 * between them. [content] is centred inside the ring (e.g. the total). An empty
 * slice list draws a faint placeholder ring.
 */
@Composable
fun TDonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 18.dp,
    gapDegrees: Float = 2f,
    content: @Composable () -> Unit = {},
) {
    val emptyColor = TallyTheme.colors.lineSoft
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = min(size.width, size.height) - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            if (slices.isEmpty()) {
                drawArc(
                    color = emptyColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
            } else {
                var start = -90f
                for (slice in slices) {
                    val sweep = slice.fraction * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = slice.color,
                            startAngle = start + gapDegrees / 2f,
                            sweepAngle = (sweep - gapDegrees).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke),
                        )
                    }
                    start += sweep
                }
            }
        }
        content()
    }
}
