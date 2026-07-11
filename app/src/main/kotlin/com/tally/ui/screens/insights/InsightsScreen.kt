package com.tally.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.core.money.Pence
import com.tally.domain.insight.CategorySpend
import com.tally.ui.components.TCard
import com.tally.ui.components.TCatDot
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TText
import com.tally.ui.components.chart.DonutSlice
import com.tally.ui.components.chart.TBarChart
import com.tally.ui.components.chart.TDonutChart
import com.tally.ui.theme.TallyTheme
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsContent(state = state, onToggleCategory = viewModel::toggleCategory, modifier = modifier)
}

@Composable
private fun InsightsContent(
    state: InsightsUiState,
    onToggleCategory: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    if (state.loaded && !state.monthHasTransactions) {
        Box(modifier = modifier.fillMaxSize().background(colors.bg0), contentAlignment = Alignment.Center) {
            TText("No spending this month yet.", color = colors.textMid)
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.bg0),
        // Extra bottom room so the last chart clears the centre Add FAB.
        contentPadding =
            PaddingValues(
                start = TallyTheme.spacing.lg,
                end = TallyTheme.spacing.lg,
                top = TallyTheme.spacing.lg,
                bottom = 96.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.xs)) {
                TText("Insights", style = TallyTheme.typography.headline)
                TText(
                    text = "${state.monthLabel} · ${state.transactionCount} transactions",
                    style = TallyTheme.typography.caption,
                    color = colors.textLo,
                )
            }
        }
        item { FilterChips(chips = state.chips, onToggle = onToggleCategory) }
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = TallyTheme.spacing.sm), contentAlignment = Alignment.Center) {
                TDonutChart(
                    slices = state.breakdown.map { DonutSlice(it.fraction, sliceColor(it.colorIndex)) },
                    modifier = Modifier.size(220.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TText("Spent", style = TallyTheme.typography.caption, color = colors.textLo)
                        TNum(amount = state.total, size = TNumSize.Medium)
                    }
                }
            }
        }
        item { ForecastCard(forecast = state.forecast) }
        items(items = state.breakdown, key = { it.categoryId ?: "uncategorised" }) { slice ->
            LegendRow(slice)
        }
        item {
            ChartCard(title = "This month, day by day") {
                TBarChart(
                    values = state.dailyTotals.map { it.value.toFloat() },
                    highlightIndex = state.todayIndex,
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                )
                DayAxis(dayCount = state.dailyTotals.size)
            }
        }
        item {
            ChartCard(title = "By weekday") {
                TBarChart(
                    values = state.weekdayTotals.map { it.value.toFloat() },
                    highlightIndex = state.peakWeekdayIndex,
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = TallyTheme.spacing.xs)) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                        TText(
                            text = label,
                            style = TallyTheme.typography.caption,
                            color = colors.textLo,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(
    chips: List<CategoryChip>,
    onToggle: (String?) -> Unit,
) {
    val colors = TallyTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { chip ->
            Row(
                modifier =
                    Modifier
                        .clip(TallyTheme.shapes.pill)
                        .background(if (chip.included) colors.accentWash else colors.bg2)
                        .border(
                            width = 1.dp,
                            color = if (chip.included) colors.accent else colors.line,
                            shape = TallyTheme.shapes.pill,
                        )
                        .clickable { onToggle(chip.id) }
                        .padding(horizontal = TallyTheme.spacing.md, vertical = TallyTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TCatDot(color = sliceColor(chip.colorIndex))
                TText(
                    text = chip.name,
                    style = TallyTheme.typography.label,
                    color = if (chip.included) colors.textHi else colors.textLo,
                )
            }
        }
    }
}

@Composable
private fun ForecastCard(forecast: Pence) {
    val colors = TallyTheme.colors
    TCard(modifier = Modifier.fillMaxWidth()) {
        TText("On pace for", style = TallyTheme.typography.caption, color = colors.textLo)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm)) {
            TNum(amount = forecast, size = TNumSize.Medium)
            TText("by month end", style = TallyTheme.typography.caption, color = colors.textLo, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    content: @Composable () -> Unit,
) {
    TCard(modifier = Modifier.fillMaxWidth()) {
        TText(text = title, style = TallyTheme.typography.label, color = TallyTheme.colors.textMid)
        Column(modifier = Modifier.padding(top = TallyTheme.spacing.sm)) { content() }
    }
}

@Composable
private fun LegendRow(slice: CategorySpend) {
    val colors = TallyTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = TallyTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
    ) {
        TCatDot(color = sliceColor(slice.colorIndex))
        TText(text = slice.name, modifier = Modifier.weight(1f))
        TText(
            text = "${(slice.fraction * 100).roundToInt()}%",
            style = TallyTheme.typography.caption,
            color = colors.textLo,
        )
        TNum(amount = slice.total, size = TNumSize.Small)
    }
}

/** Sparse day-of-month labels under the daily trend (1 … end), evenly spaced. */
@Composable
private fun DayAxis(dayCount: Int) {
    if (dayCount <= 1) return
    val marks = (0..4).map { 1 + it * (dayCount - 1) / 4 }.distinct()
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = TallyTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        marks.forEach { day ->
            TText(text = day.toString(), style = TallyTheme.typography.caption, color = TallyTheme.colors.textLo)
        }
    }
}

@Composable
private fun sliceColor(colorIndex: Int?): Color = colorIndex?.let { TallyTheme.colors.category(it) } ?: TallyTheme.colors.textDim
