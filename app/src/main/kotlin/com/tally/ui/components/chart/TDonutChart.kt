package com.tally.ui.components.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tally.ui.theme.LocalReduceMotion
import com.tally.ui.theme.TallyMotion
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
 *
 * The ring draws itself in, clockwise, the first time it appears
 * ([TallyMotion.DRAW_IN_MS]). Previews and screenshot tests show it complete.
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
    val still = LocalReduceMotion.current || LocalInspectionMode.current
    val progress = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, TallyMotion.drawIn()) }
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
                val drawnLimit = progress.value * 360f
                var start = 0f
                for (slice in slices) {
                    val sweep = slice.fraction * 360f
                    val visible = (min(start + sweep, drawnLimit) - start).coerceAtLeast(0f)
                    if (visible > gapDegrees) {
                        drawArc(
                            color = slice.color,
                            startAngle = -90f + start + gapDegrees / 2f,
                            sweepAngle = (visible - gapDegrees).coerceAtLeast(0.5f),
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
