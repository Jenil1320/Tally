package com.tally.ui.screens.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tally.ui.screens.PlaceholderScreen

/** Home tab. The live "spent today" view and recent list land in M2. */
@Composable
fun TodayScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Today",
        subtitle = "Your spending for today will show here. Tap + to add a transaction.",
        modifier = modifier,
    )
}
