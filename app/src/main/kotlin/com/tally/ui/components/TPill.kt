package com.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.ui.theme.TallyTheme

/**
 * A small rounded label for a status or a change ("+8% vs last month"). Mono,
 * because it nearly always carries a figure. [filled] adds a faint tint of
 * [color]. [showDot] puts a small dot before the text.
 */
@Composable
fun TPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TallyTheme.colors.textMid,
    filled: Boolean = true,
    showDot: Boolean = false,
) {
    val shape = TallyTheme.shapes.pill
    Row(
        modifier =
            modifier
                .clip(shape)
                .background(if (filled) color.copy(alpha = 0.16f) else Color.Transparent)
                .border(1.dp, color.copy(alpha = if (filled) 0.28f else 0.22f), shape)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showDot) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color))
        }
        TText(
            text = text,
            style = TallyTheme.typography.numericSmall.copy(fontSize = 11.sp),
            color = color,
            maxLines = 1,
        )
    }
}
