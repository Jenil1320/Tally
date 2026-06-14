package com.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tally.ui.theme.TallyTheme

/**
 * The surface atom: a rounded panel on the `bg1` surface colour, used for
 * transaction rows, insight panels, and grouped settings. Optionally bordered
 * (a hairline in `line`) and optionally tappable.
 *
 * When [onClick] is supplied the whole card becomes a button (ripple + the
 * `Button` role for TalkBack); leave it null for static content.
 */
@Composable
fun TCard(
    modifier: Modifier = Modifier,
    color: Color = TallyTheme.colors.bg1,
    shape: Shape = TallyTheme.shapes.card,
    border: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(TallyTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val lineColor = TallyTheme.colors.line
    Column(
        modifier =
            modifier
                .clip(shape)
                .background(color)
                .then(if (border) Modifier.border(1.dp, lineColor, shape) else Modifier)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .padding(contentPadding),
        content = content,
    )
}
