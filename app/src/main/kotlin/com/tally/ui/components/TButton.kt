package com.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.tally.ui.theme.TallyTheme

/**
 * The primary action atom: a filled, pill-shaped button in the active jewel
 * accent. Sized to the constitution's 44 dp minimum hit target, with a ripple
 * and the `Button` role for accessibility. Disabled state drops to the `bg3`
 * surface with dimmed text.
 *
 * This is the affirmative action only (Save, Add, Confirm). Secondary/ghost
 * styles arrive when a screen first needs them rather than speculatively.
 */
@Composable
fun TButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = TallyTheme.colors
    val background = if (enabled) colors.accent else colors.bg3
    val foreground = if (enabled) colors.onAccent else colors.textDim
    Box(
        modifier =
            modifier
                .heightIn(min = TallyTheme.spacing.minTouchTarget)
                .clip(TallyTheme.shapes.pill)
                .background(background)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = TallyTheme.spacing.xl, vertical = TallyTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        TText(text = text, style = TallyTheme.typography.label, color = foreground)
    }
}
