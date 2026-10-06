package com.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tally.core.money.Pence
import com.tally.domain.insight.CategorySpend
import com.tally.ui.components.chart.DonutSlice
import com.tally.ui.components.chart.TDonutChart
import com.tally.ui.theme.TallyTheme
import kotlin.math.roundToInt

/** The colour for a category index, or a dim neutral for uncategorised spend. */
@Composable
fun categoryColor(colorIndex: Int?): Color = colorIndex?.let { TallyTheme.colors.category(it) } ?: TallyTheme.colors.textDim

/**
 * Where the money went: a donut with the total in its centre, then every
 * category as a ranked row with a share bar. Used by Insights and by the monthly
 * report, so both read the same way.
 */
@Composable
fun CategoryBreakdownCard(
    breakdown: List<CategorySpend>,
    total: Pence,
    modifier: Modifier = Modifier,
    centerLabel: String = "Spent",
) {
    val colors = TallyTheme.colors
    TCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TDonutChart(
                slices = breakdown.map { DonutSlice(it.fraction, categoryColor(it.colorIndex)) },
                modifier = Modifier.size(184.dp),
                strokeWidth = 20.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TKicker(centerLabel)
                    TNum(amount = total, size = TNumSize.Medium, modifier = Modifier.padding(top = 2.dp))
                    TText(
                        text = if (breakdown.size == 1) "1 category" else "${breakdown.size} categories",
                        style = TallyTheme.typography.caption,
                        color = colors.textLo,
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(top = 16.dp)) {
            breakdown.forEachIndexed { index, slice ->
                if (index > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.lineSoft))
                CategoryRow(slice)
            }
        }
    }
}

@Composable
private fun CategoryRow(slice: CategorySpend) {
    val colors = TallyTheme.colors
    val color = categoryColor(slice.colorIndex)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TCatChip(name = slice.name, color = color, size = 32.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TText(
                    text = slice.name,
                    style = TallyTheme.typography.bodyStrong,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TNum(amount = slice.total, size = TNumSize.Small)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShareBar(fraction = slice.fraction, color = color, modifier = Modifier.weight(1f))
                TText(
                    text = "${(slice.fraction * 100).roundToInt()}%",
                    style = TallyTheme.typography.numericSmall,
                    color = colors.textLo,
                )
            }
        }
    }
}

/** A thin horizontal bar showing a [fraction] (0..1) in [color] over a quiet track. */
@Composable
fun ShareBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(2.dp)
    Box(modifier = modifier.height(4.dp).clip(shape).background(TallyTheme.colors.bg2)) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(shape)
                    .background(color),
        )
    }
}

/**
 * The compact version for Home: a small donut beside the top few categories.
 * Anything past [maxRows] collapses into a "+N more" line.
 */
@Composable
fun CategorySummaryCard(
    breakdown: List<CategorySpend>,
    modifier: Modifier = Modifier,
    maxRows: Int = 4,
) {
    val colors = TallyTheme.colors
    val top = breakdown.firstOrNull()
    TCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            TDonutChart(
                slices = breakdown.map { DonutSlice(it.fraction, categoryColor(it.colorIndex)) },
                modifier = Modifier.size(112.dp),
                strokeWidth = 14.dp,
            ) {
                if (top != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TKicker("Top")
                        TText(
                            text = top.name,
                            style = TallyTheme.typography.label,
                            modifier = Modifier.padding(horizontal = 18.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TText(
                            text = "${(top.fraction * 100).roundToInt()}%",
                            style = TallyTheme.typography.numericSmall,
                            color = colors.textMid,
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                breakdown.take(maxRows).forEach { slice ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TCatDot(color = categoryColor(slice.colorIndex), size = 8.dp)
                        TText(
                            text = slice.name,
                            style = TallyTheme.typography.caption,
                            color = colors.textMid,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TNum(amount = slice.total, size = TNumSize.Small)
                    }
                }
                if (breakdown.size > maxRows) {
                    TText(
                        text = "+${breakdown.size - maxRows} more",
                        style = TallyTheme.typography.caption,
                        color = colors.textLo,
                    )
                }
            }
        }
    }
}
