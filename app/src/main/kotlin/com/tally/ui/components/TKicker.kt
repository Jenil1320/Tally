package com.tally.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.tally.ui.theme.TallyTheme

/**
 * The small uppercase mono label that sits above a figure, a card or a section.
 * It names what the figure below it is, so the figure can stay quiet.
 */
@Composable
fun TKicker(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TallyTheme.colors.textLo,
) {
    TText(
        text = text.uppercase(),
        modifier = modifier,
        style = TallyTheme.typography.kicker,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
