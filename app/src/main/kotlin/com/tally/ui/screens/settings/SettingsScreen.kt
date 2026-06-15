package com.tally.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tally.ui.screens.PlaceholderScreen

/** Appearance (jewel), SMS sources, reminder, biometric, export. Lands across M4–M5. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Settings",
        subtitle = "Appearance, SMS sources, reminder, and export.",
        modifier = modifier,
    )
}
