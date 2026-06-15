package com.tally.ui.screens.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tally.ui.screens.PlaceholderScreen

/** All transactions, grouped by day. Lands in M2. */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "History",
        subtitle = "Every transaction, newest first.",
        modifier = modifier,
    )
}
