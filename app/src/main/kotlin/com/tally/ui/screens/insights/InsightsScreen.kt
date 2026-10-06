package com.tally.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.core.money.Pence
import com.tally.domain.insight.LimitState
import com.tally.domain.insight.LimitStatus
import com.tally.domain.insight.MerchantTotal
import com.tally.ui.components.CategoryBreakdownCard
import com.tally.ui.components.EatingOutCard
import com.tally.ui.components.TCard
import com.tally.ui.components.TCatChip
import com.tally.ui.components.TCatDot
import com.tally.ui.components.TKicker
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TScreenHeader
import com.tally.ui.components.TText
import com.tally.ui.components.categoryColor
import com.tally.ui.components.chart.TBarChart
import com.tally.ui.components.chart.TLimitBar
import com.tally.ui.theme.TallyTheme

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    InsightsContent(state = state, onToggleCategory = viewModel::toggleCategory, modifier = modifier)
}

@Composable
internal fun InsightsContent(
    state: InsightsUiState,
    onToggleCategory: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    if (state.loaded && !state.monthHasTransactions) {
        Box(modifier = modifier.fillMaxSize().background(colors.bg0), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TKicker("Nothing to read yet")
                TText("No spending this month yet.", color = colors.textMid)
            }
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.bg0),
        contentPadding =
            PaddingValues(
                start = TallyTheme.spacing.lg,
                end = TallyTheme.spacing.lg,
                top = TallyTheme.spacing.lg,
                bottom = TallyTheme.spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
    ) {
        item {
            TScreenHeader(
                title = "Where it went",
                kicker = "${state.monthLabel} · ${state.transactionCount} transactions",
            )
        }
        item { FilterChips(chips = state.chips, onToggle = onToggleCategory) }
        item { CategoryBreakdownCard(breakdown = state.breakdown, total = state.total) }
        if (state.eatingOut?.hasShared == true) item { EatingOutCard(summary = state.eatingOut) }
        item { ForecastCard(forecast = state.forecast) }
        if (state.limits.isNotEmpty()) {
            item { SectionKicker("Monthly limits") }
            items(items = state.limits, key = { "limit-${it.categoryId}" }) { limit -> LimitRow(limit) }
        }
        if (state.topMerchants.isNotEmpty()) {
            item { MerchantCard(merchants = state.topMerchants) }
        }
        item { DailyTrendCard(state) }
        item { WeekdayRhythmCard(state) }
        item {
            TText(
                text = "Press and hold a chart to read a single bar.",
                style = TallyTheme.typography.caption,
                color = colors.textDim,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SectionKicker(text: String) {
    TKicker(text, modifier = Modifier.padding(top = TallyTheme.spacing.sm))
}

/** Spend per day of the month; press and hold to read one day. */
@Composable
private fun DailyTrendCard(state: InsightsUiState) {
    var scrubbed by remember { mutableStateOf<Int?>(null) }
    val monthName = state.monthLabel.substringBefore(' ')
    ChartCard(
        title = "This month, day by day",
        selection =
            scrubbed?.let { index ->
                state.dailyTotals.getOrNull(index)?.let { ChartSelection("${index + 1} $monthName", it) }
            },
    ) {
        TBarChart(
            values = state.dailyTotals.map { it.value.toFloat() },
            highlightIndex = state.todayIndex,
            selectedIndex = scrubbed,
            onSelectedIndexChange = { scrubbed = it },
            modifier = Modifier.fillMaxWidth().height(96.dp),
        )
        DayAxis(dayCount = state.dailyTotals.size)
    }
}

/** Spend per weekday; press and hold to read one day. */
@Composable
private fun WeekdayRhythmCard(state: InsightsUiState) {
    var scrubbed by remember { mutableStateOf<Int?>(null) }
    ChartCard(
        title = "By weekday",
        selection =
            scrubbed?.let { index ->
                state.weekdayTotals.getOrNull(index)?.let { ChartSelection(WEEKDAY_NAMES[index], it) }
            },
    ) {
        TBarChart(
            values = state.weekdayTotals.map { it.value.toFloat() },
            highlightIndex = state.peakWeekdayIndex,
            selectedIndex = scrubbed,
            onSelectedIndexChange = { scrubbed = it },
            modifier = Modifier.fillMaxWidth().height(96.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = TallyTheme.spacing.xs)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                TText(
                    text = label,
                    style = TallyTheme.typography.caption,
                    color = TallyTheme.colors.textLo,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val WEEKDAY_NAMES =
    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** A bar the user is pressing: its label and value, shown in the chart card header. */
private data class ChartSelection(
    val label: String,
    val amount: Pence,
)

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
                        .background(if (chip.included) colors.accentWash else colors.bg1)
                        .border(
                            width = 1.dp,
                            color = if (chip.included) colors.accent else colors.lineSoft,
                            shape = TallyTheme.shapes.pill,
                        )
                        .toggleable(value = chip.included, role = Role.Checkbox, onValueChange = { onToggle(chip.id) })
                        .padding(horizontal = TallyTheme.spacing.md, vertical = TallyTheme.spacing.sm + 2.dp),
                horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TCatDot(color = categoryColor(chip.colorIndex))
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
        TKicker("On pace for")
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
        ) {
            TNum(amount = forecast, size = TNumSize.Title)
            TText(
                text = "by month end",
                style = TallyTheme.typography.caption,
                color = colors.textLo,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

/**
 * One capped category: spend against its limit, with a pace tick, and what is
 * left restated per day — the form that is usable at the till. Voice stays
 * observational: it reports the position and never grades it.
 */
@Composable
private fun LimitRow(limit: LimitState) {
    val colors = TallyTheme.colors
    val past = limit.status == LimitStatus.PAST
    TCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md)) {
            TCatChip(name = limit.name, color = categoryColor(limit.colorIndex), size = 32.dp)
            TText(text = limit.name, style = TallyTheme.typography.bodyStrong, modifier = Modifier.weight(1f))
            TNum(amount = limit.spent, size = TNumSize.Small)
            TText(text = "of", style = TallyTheme.typography.caption, color = colors.textLo)
            TNum(amount = limit.limit, size = TNumSize.Small, color = colors.textLo)
        }
        TLimitBar(
            fillFraction = limit.fillFraction,
            paceFraction = limit.paceFraction,
            fillColor = categoryColor(limit.colorIndex),
            // Padding first: with the height set before it, the 8 dp of padding left the bar no room to draw.
            modifier = Modifier.fillMaxWidth().padding(top = TallyTheme.spacing.md).height(8.dp),
        )
        Row(
            modifier = Modifier.padding(top = TallyTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.xs),
        ) {
            if (past) {
                TNum(amount = Pence(-limit.remaining.value), size = TNumSize.Small, color = colors.warning)
                TText(text = "past the limit", style = TallyTheme.typography.caption, color = colors.textLo)
            } else {
                TNum(amount = limit.dailyAllowance, size = TNumSize.Small, color = colors.accent)
                TText(text = "a day for the rest of the month", style = TallyTheme.typography.caption, color = colors.textLo)
            }
        }
        if (!past && limit.projectedPast) {
            Row(
                modifier = Modifier.padding(top = TallyTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.xs),
            ) {
                TText(text = "at this pace,", style = TallyTheme.typography.caption, color = colors.textLo)
                TNum(amount = limit.projected, size = TNumSize.Small, color = colors.textMid)
                TText(text = "by month end", style = TallyTheme.typography.caption, color = colors.textLo)
            }
        }
    }
}

/**
 * Spend grouped by merchant. The category donut hides "small and often" — many
 * modest purchases at one place — which this card makes visible by pairing each
 * total with its visit count.
 */
@Composable
private fun MerchantCard(merchants: List<MerchantTotal>) {
    val colors = TallyTheme.colors
    TCard(modifier = Modifier.fillMaxWidth()) {
        TKicker("Where it goes, by merchant")
        merchants.forEachIndexed { index, merchant ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = if (index == 0) 12.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
            ) {
                TText(
                    text = (index + 1).toString().padStart(2, '0'),
                    style = TallyTheme.typography.numericSmall,
                    color = colors.textDim,
                )
                Column(modifier = Modifier.weight(1f)) {
                    TText(
                        text = merchant.merchant,
                        style = TallyTheme.typography.bodyStrong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TText(
                        text = if (merchant.visits == 1) "once" else "${merchant.visits} times",
                        style = TallyTheme.typography.caption,
                        color = colors.textLo,
                    )
                }
                TNum(amount = merchant.total, size = TNumSize.Small)
            }
        }
    }
}

/**
 * A titled chart panel. While the user scrubs, [selection] replaces the title
 * with that bar's label and amount, so the reading appears without shifting the
 * layout underneath their finger.
 */
@Composable
private fun ChartCard(
    title: String,
    selection: ChartSelection? = null,
    content: @Composable () -> Unit,
) {
    val colors = TallyTheme.colors
    TCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TKicker(
                text = selection?.label ?: title,
                color = if (selection != null) colors.textHi else colors.textLo,
            )
            if (selection != null) {
                TNum(amount = selection.amount, size = TNumSize.Small, color = colors.accent)
            }
        }
        Column(modifier = Modifier.padding(top = TallyTheme.spacing.md)) { content() }
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
