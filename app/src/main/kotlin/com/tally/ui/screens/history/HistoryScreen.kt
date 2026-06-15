package com.tally.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TText
import com.tally.ui.components.TTransactionRow
import com.tally.ui.theme.TallyTheme

@Composable
fun HistoryScreen(
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(state = state, onTransactionClick = onTransactionClick, modifier = modifier)
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    Box(modifier = modifier.fillMaxSize().background(colors.bg0)) {
        if (state.loaded && state.days.isEmpty()) {
            TText(
                text = "No transactions yet.",
                style = TallyTheme.typography.body,
                color = colors.textMid,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = TallyTheme.spacing.lg,
                        end = TallyTheme.spacing.lg,
                        top = TallyTheme.spacing.md,
                        bottom = TallyTheme.spacing.xxl,
                    ),
                verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
            ) {
                state.days.forEach { day ->
                    item(key = "header-${day.label}") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = TallyTheme.spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TText(text = day.label, style = TallyTheme.typography.label, color = colors.textMid)
                            TNum(amount = day.total, size = TNumSize.Small, color = colors.textLo)
                        }
                    }
                    items(count = day.items.size, key = { day.items[it].id }) { index ->
                        val rowItem = day.items[index]
                        TTransactionRow(item = rowItem, onClick = { onTransactionClick(rowItem.id) })
                    }
                }
            }
        }
    }
}
