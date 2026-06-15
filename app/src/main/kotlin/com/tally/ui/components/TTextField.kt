package com.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import com.tally.ui.theme.TallyTheme

/**
 * The text-input atom: a single-line field on the `bg3` surface with a themed
 * placeholder and an accent cursor. Built on [BasicTextField] for full control
 * of the dark styling rather than fighting Material's defaults.
 */
@Composable
fun TTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = TallyTheme.colors
    Box(
        modifier =
            modifier
                .clip(TallyTheme.shapes.small)
                .background(colors.bg3)
                .heightIn(min = TallyTheme.spacing.minTouchTarget)
                .padding(horizontal = TallyTheme.spacing.md, vertical = TallyTheme.spacing.sm),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            textStyle = TallyTheme.typography.body.copy(color = colors.textHi),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    TText(text = placeholder, style = TallyTheme.typography.body, color = colors.textDim)
                }
                innerTextField()
            },
        )
    }
}
