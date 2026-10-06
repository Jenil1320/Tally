package com.tally.ui.screens.transactions

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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.ui.components.TCard
import com.tally.ui.components.TFab
import com.tally.ui.components.TKicker
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TScreenHeader
import com.tally.ui.components.TText
import com.tally.ui.components.TTransactionRow
import com.tally.ui.theme.TallyTheme

/**
 * Every transaction, newest first, grouped by day. This is the one screen with
 * an add button: the round button sits at the bottom right, above the list.
 */
@Composable
fun TransactionsScreen(
    onTransactionClick: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    TransactionsContent(
        state = state,
        onTransactionClick = onTransactionClick,
        onAddClick = onAddClick,
        modifier = modifier,
    )
}

@Composable
internal fun TransactionsContent(
    state: TransactionsUiState,
    onTransactionClick: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    Box(modifier = modifier.fillMaxSize().background(colors.bg0)) {
        if (state.loaded && state.days.isEmpty()) {
            Column(modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp)) {
                TKicker("Nothing here yet", modifier = Modifier.align(Alignment.CenterHorizontally))
                TText(
                    text = "Tap + to log your first transaction.",
                    color = colors.textMid,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            TScreenHeader(
                title = "Transactions",
                kicker = "All activity",
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = TallyTheme.spacing.lg,
                        end = TallyTheme.spacing.lg,
                        top = TallyTheme.spacing.lg,
                        // Room so the last row clears the add button.
                        bottom = 104.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
            ) {
                item(key = "header") {
                    TScreenHeader(title = "Transactions", kicker = "All activity", modifier = Modifier.padding(bottom = 4.dp))
                }
                items(items = state.days, key = { it.key }) { day ->
                    Column(
                        modifier = Modifier.animateItem(),
                        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TText(text = day.label, style = TallyTheme.typography.label, color = colors.textMid)
                            TNum(amount = day.total, size = TNumSize.Small, color = colors.textLo)
                        }
                        TCard(contentPadding = PaddingValues(0.dp)) {
                            day.items.forEachIndexed { index, item ->
                                TTransactionRow(
                                    item = item,
                                    onClick = { onTransactionClick(item.id) },
                                    showDivider = index < day.items.lastIndex,
                                )
                            }
                        }
                    }
                }
            }
        }
        TFab(
            onClick = onAddClick,
            contentDescription = "Add transaction",
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp),
        )
    }
}
