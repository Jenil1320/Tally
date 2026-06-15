package com.tally.ui.screens.insights

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.domain.insight.CategorySpend
import com.tally.ui.components.TCatDot
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TText
import com.tally.ui.components.chart.DonutSlice
import com.tally.ui.components.chart.TDonutChart
import com.tally.ui.theme.TallyTheme
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsContent(state = state, modifier = modifier)
}

@Composable
private fun InsightsContent(
    state: InsightsUiState,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    if (state.loaded && state.breakdown.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().background(colors.bg0), contentAlignment = Alignment.Center) {
            TText("No spending this month yet.", color = colors.textMid)
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.bg0),
        contentPadding = PaddingValues(TallyTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.xs)) {
                TText("Insights", style = TallyTheme.typography.headline)
                TText(state.monthLabel, style = TallyTheme.typography.caption, color = colors.textLo)
            }
        }
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = TallyTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
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
        items(items = state.breakdown, key = { it.categoryId ?: "uncategorised" }) { slice ->
            LegendRow(slice)
        }
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

@Composable
private fun sliceColor(colorIndex: Int?): Color = colorIndex?.let { TallyTheme.colors.category(it) } ?: TallyTheme.colors.textDim
