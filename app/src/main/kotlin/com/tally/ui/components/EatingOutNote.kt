package com.tally.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tally.domain.insight.EatingOutSummary
import com.tally.ui.theme.TallyTheme

/**
 * One quiet line for awareness: what the user's own share of eating out came to,
 * against what was paid. Cash flow elsewhere on the screen still counts the full
 * amount (ADR-0010). Shows nothing until at least one meal has a recorded share.
 */
@Composable
fun EatingOutNote(
    summary: EatingOutSummary?,
    modifier: Modifier = Modifier,
) {
    if (summary == null || !summary.hasShared) return
    val colors = TallyTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TText(text = "Eating out, your share", style = TallyTheme.typography.caption, color = colors.textLo)
        TNum(amount = summary.mine, size = TNumSize.Small, color = colors.accent)
        TText(text = "of", style = TallyTheme.typography.caption, color = colors.textLo)
        TNum(amount = summary.paid, size = TNumSize.Small, color = colors.textLo)
    }
}

/**
 * The same awareness as a card, for Insights and Reports. It names the two
 * figures and says what others covered, so the gap is not a mystery.
 */
@Composable
fun EatingOutCard(
    summary: EatingOutSummary?,
    modifier: Modifier = Modifier,
) {
    if (summary == null || !summary.hasShared) return
    val colors = TallyTheme.colors
    TCard(modifier = modifier.fillMaxWidth()) {
        TKicker("Eating out · your share")
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TNum(amount = summary.mine, size = TNumSize.Large, color = colors.accent)
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TText(text = "of", style = TallyTheme.typography.caption, color = colors.textLo)
            TNum(amount = summary.paid, size = TNumSize.Small, color = colors.textMid)
            TText(
                text = "paid. Others covered",
                style = TallyTheme.typography.caption,
                color = colors.textLo,
            )
            TNum(amount = summary.coveredByOthers, size = TNumSize.Small, color = colors.textMid)
        }
        TText(
            text = "Cash flow still counts the full amount paid.",
            style = TallyTheme.typography.caption,
            color = colors.textDim,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
