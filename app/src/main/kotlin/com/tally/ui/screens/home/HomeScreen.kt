package com.tally.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.core.money.Pence
import com.tally.ui.components.CategorySummaryCard
import com.tally.ui.components.EatingOutNote
import com.tally.ui.components.TButton
import com.tally.ui.components.TCard
import com.tally.ui.components.TKicker
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TPill
import com.tally.ui.components.TScreenHeader
import com.tally.ui.components.TSectionHeader
import com.tally.ui.components.TText
import com.tally.ui.components.TTransactionRow
import com.tally.ui.theme.TallyTheme
import kotlin.math.abs

/**
 * Home: this month at a glance. The big figure is what has been spent so far,
 * followed by where it went, today, the month-end forecast, and the latest
 * entries. There is no add button here; adding lives on Transactions.
 */
@Composable
fun HomeScreen(
    onTransactionClick: (String) -> Unit,
    onOpenTransactions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    HomeContent(
        state = state,
        onTransactionClick = onTransactionClick,
        onOpenTransactions = onOpenTransactions,
        modifier = modifier,
    )
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onTransactionClick: (String) -> Unit,
    onOpenTransactions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    if (state.loaded && !state.hasAnyTransactions) {
        HomeEmpty(onOpenTransactions = onOpenTransactions, modifier = modifier)
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
        item { TScreenHeader(title = state.monthName, kicker = state.dateKicker) }
        item { Hero(state) }
        if (state.breakdown.isNotEmpty()) item { CategorySummaryCard(breakdown = state.breakdown) }
        item { StatTiles(state) }
        if (state.recent.isNotEmpty()) {
            item {
                TSectionHeader(title = "Recent activity", action = "See all", onAction = onOpenTransactions)
            }
            item {
                TCard(contentPadding = PaddingValues(0.dp)) {
                    state.recent.forEachIndexed { index, row ->
                        TTransactionRow(
                            item = row,
                            onClick = { onTransactionClick(row.id) },
                            showDivider = index < state.recent.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(state: HomeUiState) {
    val colors = TallyTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        TKicker("Spent this month")
        TNum(
            amount = state.monthTotal,
            size = TNumSize.Hero,
            animate = true,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.deltaPercent?.let { delta ->
                val up = delta > 0
                TPill(
                    text = "${if (up) "+" else ""}$delta% vs ${state.lastMonthName} so far",
                    color = if (up) colors.warning else colors.positive,
                    showDot = true,
                )
            }
            TText(
                text = if (state.daysLeft == 0) "last day" else "${state.daysLeft} days left",
                style = TallyTheme.typography.caption,
                color = colors.textLo,
            )
        }
        EatingOutNote(summary = state.eatingOut, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun StatTiles(state: HomeUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md)) {
        StatTile(
            kicker = "Today",
            amount = state.spentToday,
            note = if (state.todayCount == 1) "1 transaction" else "${state.todayCount} transactions",
            modifier = Modifier.weight(1f),
        )
        val gap = state.forecastGap
        StatTile(
            kicker = "At this rate",
            amount = state.forecast,
            noteAmount = gap?.let { Pence(abs(it.value)) }?.takeIf { it.value > 0 },
            note =
                when {
                    gap == null -> "by month end"
                    gap.value < 0 -> "under ${state.lastMonthName}"
                    gap.value > 0 -> "over ${state.lastMonthName}"
                    else -> "level with ${state.lastMonthName}"
                },
            accent = true,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A small stat card. [noteAmount], when set, is drawn through `TNum` just before [note]. */
@Composable
private fun StatTile(
    kicker: String,
    amount: Pence,
    note: String,
    modifier: Modifier = Modifier,
    noteAmount: Pence? = null,
    accent: Boolean = false,
) {
    val colors = TallyTheme.colors
    TCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        TKicker(kicker)
        TNum(
            amount = amount,
            size = TNumSize.Title,
            color = if (accent) colors.accent else colors.textHi,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (noteAmount != null) TNum(amount = noteAmount, size = TNumSize.Small, color = colors.textMid)
            TText(text = note, style = TallyTheme.typography.caption, color = colors.textLo)
        }
    }
}

@Composable
private fun HomeEmpty(
    onOpenTransactions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    Box(modifier = modifier.fillMaxSize().background(colors.bg0), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TKicker("Nothing logged yet")
            TText(
                text = "Your month will take shape here once you log a transaction.",
                color = colors.textMid,
                textAlign = TextAlign.Center,
            )
            TButton(text = "Go to Transactions", onClick = onOpenTransactions)
        }
    }
}
