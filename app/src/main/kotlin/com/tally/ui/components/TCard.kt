package com.tally.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tally.ui.theme.TallyMotion
import com.tally.ui.theme.TallyTheme

/**
 * The surface atom: a rounded panel on the `bg1` surface colour, used for
 * transaction rows, insight panels, and grouped settings. It carries a soft
 * hairline ([border]) rather than a shadow, so elevation comes from luminance
 * and the screen stays flat and calm.
 *
 * When [onClick] is supplied the whole card becomes a button (ripple, a slight
 * press-in, and the `Button` role for TalkBack); leave it null for static content.
 */
@Composable
fun TCard(
    modifier: Modifier = Modifier,
    color: Color = TallyTheme.colors.bg1,
    shape: Shape = TallyTheme.shapes.card,
    border: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(TallyTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val lineColor = TallyTheme.colors.lineSoft
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) PRESSED_SCALE else 1f,
        animationSpec = TallyMotion.standard(),
        label = "cardPress",
    )
    Column(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(color)
                .then(if (border) Modifier.border(1.dp, lineColor, shape) else Modifier)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            role = Role.Button,
                            onClickLabel = onClickLabel,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(contentPadding),
        content = content,
    )
}

private const val PRESSED_SCALE = 0.985f
