package com.tally.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.tally.ui.components.TText
import com.tally.ui.theme.TallyTheme

/**
 * Shared empty-state scaffold for screens whose real content lands in later
 * milestones (M2–M5). Keeps the navigation graph fully wired and walkable now,
 * with each destination clearly labelled.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.bg0)
                .padding(TallyTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TText(text = title, style = TallyTheme.typography.headline)
        TText(
            text = subtitle,
            style = TallyTheme.typography.body,
            color = colors.textMid,
            textAlign = TextAlign.Center,
        )
    }
}
