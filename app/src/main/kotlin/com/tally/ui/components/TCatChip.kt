package com.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.ui.theme.TallyTheme

/** The two-letter mark for a category: "Eating out" gives "Ea". Uncategorised gives two dots. */
fun categoryGlyph(name: String?): String {
    val letters = name?.filter { it.isLetter() }.orEmpty()
    return if (letters.isEmpty()) "··" else letters.take(2).lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * A category's identity on a row or a card: a rounded tile tinted in the
 * category colour, with a two-letter mark. The colour repeats the label beside
 * it, so the chip is hidden from TalkBack. For a small marker beside a label,
 * use [TCatDot] instead.
 */
@Composable
fun TCatChip(
    name: String?,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier =
            modifier
                .clearAndSetSemantics { }
                .size(size)
                .clip(shape)
                .background(color.copy(alpha = 0.18f))
                .border(1.dp, color.copy(alpha = 0.36f), shape),
        contentAlignment = Alignment.Center,
    ) {
        TText(
            text = categoryGlyph(name),
            style =
                TallyTheme.typography.numericSmall.copy(
                    fontSize = (size.value * 0.32f).sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = color,
            maxLines = 1,
        )
    }
}
