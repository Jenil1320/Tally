package com.tally.ui.screens.insights

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tally.ui.screens.PlaceholderScreen

/** Categories, trend, rhythm, forecast. Lands in M3. */
@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Insights",
        subtitle = "Category breakdown, trend, and rhythm will appear here.",
        modifier = modifier,
    )
}
