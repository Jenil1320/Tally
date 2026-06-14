package com.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The category marker: a small filled dot in a category's colour, sitting
 * beside its label in rows, the legend, and the picker. Pull [color] from
 * `TallyTheme.colors.category(index)` so categories stay visually stable.
 *
 * Decorative by default (the colour repeats the adjacent text label, so it is
 * hidden from TalkBack). Pass [contentDescription] only when the dot is the
 * *sole* carrier of the category's meaning.
 */
@Composable
fun TCatDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    contentDescription: String? = null,
) {
    val semantics =
        if (contentDescription != null) {
            Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
        } else {
            Modifier.clearAndSetSemantics { }
        }
    Box(
        modifier =
            modifier
                .then(semantics)
                .size(size)
                .clip(CircleShape)
                .background(color),
    )
}
