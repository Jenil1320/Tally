package com.tally.ui.components.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tally.ui.theme.TallyTheme
import kotlin.math.floor

/**
 * Which bar sits under [x] for a chart [width] wide holding [count] bars, or null
 * when there is nothing to hit. Mirrors the layout maths in [TBarChart] (equal
 * bars separated by a gap of a quarter-bar-slot), so scrubbing lands on the bar
 * the user is actually touching. Pure, so it is unit-tested directly.
 */
internal fun barIndexAt(
    x: Float,
    width: Float,
    count: Int,
): Int? {
    if (count <= 0 || width <= 0f) return null
    val gap = width / (count * 4f)
    val barWidth = (width - gap * (count - 1)) / count
    val slot = barWidth + gap
    if (slot <= 0f) return null
    return floor(x / slot).toInt().coerceIn(0, count - 1)
}

/**
 * A hand-rolled vertical bar chart (Compose Canvas — no chart library). Bars are
 * normalised to the largest value and grow from a baseline; an optional
 * [highlightIndex] paints one bar in the accent (e.g. today, or the peak day).
 * Zero values render as a faint stub so gaps stay legible.
 *
 * Pass [onSelectedIndexChange] to make the chart scrubbable: a long press picks
 * the bar under the finger (with a haptic tick) and dragging moves the selection,
 * so the caller can show that bar's value; releasing clears it by reporting null.
 * [selectedIndex] is drawn in [highlightColor] and takes precedence over
 * [highlightIndex].
 */
@Composable
fun TBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = TallyTheme.colors.accentDim,
    highlightIndex: Int? = null,
    highlightColor: Color = TallyTheme.colors.accent,
    selectedIndex: Int? = null,
    onSelectedIndexChange: ((Int?) -> Unit)? = null,
) {
    val stubColor = TallyTheme.colors.bg3
    val haptics = LocalHapticFeedback.current

    val gestures =
        if (onSelectedIndexChange == null) {
            Modifier
        } else {
            Modifier.pointerInput(values.size) {
                var current: Int? = null

                fun report(index: Int?) {
                    if (index != current) {
                        current = index
                        if (index != null) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectedIndexChange(index)
                    }
                }
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> report(barIndexAt(offset.x, size.width.toFloat(), values.size)) },
                    onDrag = { change, _ -> report(barIndexAt(change.position.x, size.width.toFloat(), values.size)) },
                    onDragEnd = { report(null) },
                    onDragCancel = { report(null) },
                )
            }
        }

    Canvas(modifier = modifier.then(gestures)) {
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
                    when {
                        index == selectedIndex -> highlightColor
                        value <= 0f -> stubColor
                        index == highlightIndex -> highlightColor
                        else -> barColor
                    },
                topLeft = Offset(x, size.height - drawHeight),
                size = Size(barWidth, drawHeight),
                cornerRadius = radius,
            )
        }
    }
}
