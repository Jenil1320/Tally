package com.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.ui.theme.TallyTheme

/**
 * The amount keypad. Digits fill in from the right as pence (tap 4, 8, 5 →
 * £4.85), so there is no decimal key and no invalid intermediate state. `00` is
 * a convenience for round amounts; the arrow deletes the last digit.
 *
 * Callbacks are intentionally low-level (the ViewModel owns the running pence
 * value) so the keypad stays a dumb, testable component. Keys are quiet raised
 * tiles with a hairline edge, set in mono like every other figure.
 */
@Composable
fun TKeypad(
    onDigit: (Int) -> Unit,
    onDoubleZero: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
    ) {
        for (row in listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))) {
            Row(horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm)) {
                row.forEach { digit ->
                    Key(label = digit.toString(), onClick = { onDigit(digit) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm)) {
            Key(label = "00", onClick = onDoubleZero)
            Key(label = "0", onClick = { onDigit(0) })
            Key(label = null, contentDescription = "Delete", onClick = onBackspace) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = null,
                    tint = TallyTheme.colors.textMid,
                )
            }
        }
    }
}

@Composable
private fun RowScope.Key(
    label: String?,
    onClick: () -> Unit,
    contentDescription: String? = label,
    content: @Composable () -> Unit = {
        if (label != null) {
            TText(text = label, style = TallyTheme.typography.numericLarge.copy(fontSize = KEY_FONT_SIZE))
        }
    },
) {
    val shape = TallyTheme.shapes.large
    Box(
        modifier =
            Modifier
                .weight(1f)
                .height(52.dp)
                .clip(shape)
                .background(TallyTheme.colors.bg1)
                .border(1.dp, TallyTheme.colors.lineSoft, shape)
                .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private val KEY_FONT_SIZE = 22.sp
