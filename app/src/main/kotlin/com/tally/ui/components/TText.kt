package com.tally.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.tally.ui.theme.TallyTheme

/**
 * The base text atom. A thin wrapper over Material 3 [Text] that defaults to
 * Tally's body style and high-emphasis text colour, so screen code never spells
 * out a font or colour for ordinary prose. Use the `TallyTheme.typography.*`
 * styles for other roles (titles, captions, labels).
 *
 * Numbers do **not** go through here — money and counts render via `TNum`.
 */
@Composable
fun TText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TallyTheme.typography.body,
    color: Color = TallyTheme.colors.textHi,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
    )
}
