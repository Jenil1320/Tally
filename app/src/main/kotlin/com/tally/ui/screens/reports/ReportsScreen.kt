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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.tally.domain.report.MonthlyReport
import com.tally.ui.components.CategoryBreakdownCard
import com.tally.ui.components.EatingOutCard
import com.tally.ui.components.TCard
import com.tally.ui.components.TKicker
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TPill
import com.tally.ui.components.TText
import com.tally.ui.theme.TallyTheme
import kotlinx.datetime.LocalDate

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    ReportsContent(
        state = state,
        onPrev = viewModel::previousMonth,
        onNext = viewModel::nextMonth,
        modifier = modifier,
    )
}

@Composable
internal fun ReportsContent(
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
                bottom = TallyTheme.spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
    ) {
        item {
            MonthHeader(
                label = report?.monthLabel ?: "",
                kicker =
                    if (report == null || report.isEmpty) {
                        "Monthly report"
                    } else {
                        "Monthly report · ${report.transactionCount} transactions"
                    },
                canGoForward = state.canGoForward,
                onPrev = onPrev,
                onNext = onNext,
            )
        }

        if (state.loaded && (report == null || report.isEmpty)) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = TallyTheme.spacing.xxl),
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
        item { CategoryBreakdownCard(breakdown = report.byCategory, total = report.total) }
        if (report.eatingOut?.hasShared == true) item { EatingOutCard(summary = report.eatingOut) }
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
    kicker: String,
    canGoForward: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NavArrow(glyph = "‹", description = "Previous month", enabled = true, onClick = onPrev)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            TKicker(kicker)
            TText(
                text = label.ifEmpty { "Reports" },
                style = TallyTheme.typography.headline,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        NavArrow(glyph = "›", description = "Next month", enabled = canGoForward, onClick = onNext)
    }
}

@Composable
private fun NavArrow(
    glyph: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = TallyTheme.colors
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.bg1)
                .then(if (enabled) Modifier.clickable(role = Role.Button, onClickLabel = description, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        TText(text = glyph, style = TallyTheme.typography.title, color = if (enabled) colors.textMid else colors.textDim)
    }
}

@Composable
private fun TotalCard(report: MonthlyReport) {
    val colors = TallyTheme.colors
    TCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        TKicker("Spent this month")
        TNum(amount = report.total, size = TNumSize.Hero, modifier = Modifier.padding(top = 6.dp))
        report.vsPrevious?.let { delta ->
            val up = delta.deltaPercent >= 0
            val sign = if (up) "+" else ""
            TPill(
                text = "$sign${delta.deltaPercent}% vs the month before",
                color = if (up) colors.warning else colors.positive,
                showDot = true,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun ObservationCard(text: String) {
    val colors = TallyTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(TallyTheme.shapes.card)
                .background(colors.accentWash)
                .padding(TallyTheme.spacing.lg),
    ) {
        TKicker("Something to notice", color = colors.accent)
        TText(
            text = text,
            style = TallyTheme.typography.bodyStrong,
            color = colors.textHi,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun NotableGrid(report: MonthlyReport) {
    Column(verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md)) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md)) {
            StatTile(label = "Avg / day", amount = report.avgPerDay, sub = "across the month", modifier = Modifier.weight(1f))
            StatTile(
                label = "Auto-captured",
                value = "${report.autoCapturedPercent}%",
                mono = true,
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
    mono: Boolean = false,
    amount: Pence? = null,
    sub: String,
) {
    val colors = TallyTheme.colors
    TCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        TKicker(label)
        Box(modifier = Modifier.padding(top = 6.dp)) {
            when {
                amount != null -> TNum(amount = amount, size = TNumSize.Title)
                else ->
                    TText(
                        text = value.orEmpty(),
                        style = if (mono) TallyTheme.typography.numericTitle else TallyTheme.typography.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
            }
        }
        TText(text = sub, style = TallyTheme.typography.caption, color = colors.textLo, modifier = Modifier.padding(top = 4.dp))
    }
}

private val MONTH_ABBR =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun dayShort(date: LocalDate): String = "${date.dayOfMonth} ${MONTH_ABBR[date.monthNumber - 1]}"
