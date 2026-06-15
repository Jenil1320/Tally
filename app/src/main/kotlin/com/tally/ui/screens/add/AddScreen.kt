package com.tally.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.tally.ui.components.TButton
import com.tally.ui.components.TText
import com.tally.ui.theme.TallyTheme

/**
 * FAB target. A full-screen placeholder for now; M2 replaces it with the Add
 * bottom sheet (amount keypad, category, splits).
 */
@Composable
fun AddScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.bg0)
                .padding(TallyTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TText(text = "Add transaction", style = TallyTheme.typography.headline)
        TText(
            text = "The amount keypad, category, and splits arrive in M2.",
            style = TallyTheme.typography.body,
            color = colors.textMid,
            textAlign = TextAlign.Center,
        )
        TButton(text = "Close", onClick = onClose)
    }
}
