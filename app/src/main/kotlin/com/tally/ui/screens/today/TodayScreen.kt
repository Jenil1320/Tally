package com.tally.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayContent(state = state, modifier = modifier)
}

@Composable
private fun TodayContent(
    state: TodayUiState,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    Column(
        modifier = modifier.fillMaxSize().background(colors.bg0),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(TallyTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.xs),
        ) {
            TText(text = "Spent today", style = TallyTheme.typography.caption, color = colors.textLo)
            TNum(amount = state.spentToday, size = TNumSize.Large)
        }

        if (state.loaded && state.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TText(
                    text = "Nothing yet today. Tap + to add a transaction.",
                    style = TallyTheme.typography.body,
                    color = colors.textMid,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = TallyTheme.spacing.lg,
                        end = TallyTheme.spacing.lg,
                        bottom = TallyTheme.spacing.xxl,
                    ),
                verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
            ) {
                items(items = state.items, key = { it.id }) { item ->
                    TTransactionRow(item = item)
                }
            }
        }
    }
}
