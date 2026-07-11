package com.tally.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.tally.domain.report.MonthlyReport
import com.tally.ui.components.TCard
import com.tally.ui.components.TCatDot
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TText
import com.tally.ui.components.chart.DonutSlice
import com.tally.ui.components.chart.TDonutChart
import com.tally.ui.theme.TallyTheme
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsContent(
        state = state,
        onPrev = viewModel::previousMonth,
        onNext = viewModel::nextMonth,
        modifier = modifier,
    )
}

@Composable
private fun ReportsContent(
    state: ReportsUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    val report = state.report
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.bg0),
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
            MonthHeader(
                label = report?.monthLabel ?: "",
                subtitle =
                    if (report == null || report.isEmpty) {
                        "Monthly report"
                    } else {
                        "${report.transactionCount} transactions"
                    },
                canGoForward = state.canGoForward,
                onPrev = onPrev,
                onNext = onNext,
            )
        }

        if (state.loaded && (report == null || report.isEmpty)) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = TallyTheme.spacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    TText("No spending in ${report?.monthLabel ?: "this month"}.", color = colors.textMid)
                }
            }
            return@LazyColumn
        }

        if (report == null) return@LazyColumn

        item { TotalCard(report) }
        if (state.observation.isNotEmpty()) item { ObservationCard(state.observation) }

        item {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = TallyTheme.spacing.sm), contentAlignment = Alignment.Center) {
                TDonutChart(
                    slices = report.byCategory.map { DonutSlice(it.fraction, sliceColor(it.colorIndex)) },
                    modifier = Modifier.size(200.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TText("Spent", style = TallyTheme.typography.caption, color = colors.textLo)
                        TNum(amount = report.total, size = TNumSize.Medium)
                    }
                }
            }
        }
        items(items = report.byCategory, key = { it.categoryId ?: "uncategorised" }) { slice -> LegendRow(slice) }

        item { NotableGrid(report) }

        item {
            TText(
                text = "Delivered on the last day of each month.",
                style = TallyTheme.typography.caption,
                color = colors.textDim,
                modifier = Modifier.fillMaxWidth().padding(top = TallyTheme.spacing.sm),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthHeader(
    label: String,
    subtitle: String,
    canGoForward: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = TallyTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NavArrow(glyph = "‹", enabled = true, onClick = onPrev)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            TText(text = label.ifEmpty { "Reports" }, style = TallyTheme.typography.headline)
            TText(text = subtitle, style = TallyTheme.typography.caption, color = colors.textLo)
        }
        NavArrow(glyph = "›", enabled = canGoForward, onClick = onNext)
    }
}

@Composable
private fun NavArrow(
    glyph: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = TallyTheme.colors
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        TText(text = glyph, style = TallyTheme.typography.headline, color = if (enabled) colors.textMid else colors.textDim)
    }
}

@Composable
private fun TotalCard(report: MonthlyReport) {
    val colors = TallyTheme.colors
    TCard(modifier = Modifier.fillMaxWidth()) {
        TText("Spent this month", style = TallyTheme.typography.caption, color = colors.textLo)
        TNum(amount = report.total, size = TNumSize.Large, modifier = Modifier.padding(top = 2.dp))
        report.vsPrevious?.let { delta ->
            val up = delta.deltaPercent >= 0
            val sign = if (up) "+" else ""
            TText(
                text = "$sign${delta.deltaPercent}% vs the month before",
                style = TallyTheme.typography.caption,
                color = if (up) colors.warning else colors.positive,
                modifier = Modifier.padding(top = TallyTheme.spacing.xs),
            )
        }
    }
}

@Composable
private fun ObservationCard(text: String) {
    val colors = TallyTheme.colors
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(TallyTheme.shapes.card)
                .background(colors.accentWash)
                .padding(TallyTheme.spacing.md),
    ) {
        TText(text = text, color = colors.textHi)
    }
}

@Composable
private fun NotableGrid(report: MonthlyReport) {
    Column(verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm)) {
            report.topMerchant?.let { m ->
                StatTile(
                    label = "Top merchant",
                    value = m.merchant,
                    sub = "${m.visits} ${if (m.visits == 1) "visit" else "visits"}",
                    modifier = Modifier.weight(1f),
                )
            }
            report.priciestDay?.let { d ->
                StatTile(label = "Priciest day", amount = d.total, sub = dayShort(d.date), modifier = Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm)) {
            StatTile(label = "Avg / day", amount = report.avgPerDay, sub = "across the month", modifier = Modifier.weight(1f))
            StatTile(
                label = "Auto-captured",
                value = "${report.autoCapturedPercent}%",
                sub = "from notifications",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    amount: Pence? = null,
    sub: String,
) {
    val colors = TallyTheme.colors
    TCard(modifier = modifier) {
        TText(text = label, style = TallyTheme.typography.label, color = colors.textLo)
        Box(modifier = Modifier.padding(top = TallyTheme.spacing.xs)) {
            when {
                amount != null -> TNum(amount = amount, size = TNumSize.Small)
                else -> TText(text = value.orEmpty(), style = TallyTheme.typography.body)
            }
        }
        TText(text = sub, style = TallyTheme.typography.caption, color = colors.textLo, modifier = Modifier.padding(top = 2.dp))
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
        TText(text = "${(slice.fraction * 100).roundToInt()}%", style = TallyTheme.typography.caption, color = colors.textLo)
        TNum(amount = slice.total, size = TNumSize.Small)
    }
}

private val MONTH_ABBR =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun dayShort(date: LocalDate): String = "${date.dayOfMonth} ${MONTH_ABBR[date.monthNumber - 1]}"

@Composable
private fun sliceColor(colorIndex: Int?): Color = colorIndex?.let { TallyTheme.colors.category(it) } ?: TallyTheme.colors.textDim
