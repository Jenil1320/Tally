package com.tally.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.tally.core.money.MoneyFormat
import com.tally.core.money.Pence
import com.tally.ui.theme.TallyTheme

/** Size variants for [TNum], mapped to the mono numeric type scale. */
enum class TNumSize { Large, Medium, Small }

/**
 * The money atom — the *only* sanctioned way to put a GBP amount on screen
 * (constitution: never `Text("£$n")`). Renders [amount] in Geist Mono with
 * tabular figures so amounts in a column align and don't jitter as they change.
 *
 * Sign handling matches an expense ledger: negatives always carry a leading
 * `-`. Set [signed] to also show a leading `+` on positive values (used for
 * credits/refunds). Colour is the caller's choice — pass
 * `TallyTheme.colors.positive` for money in, leave default for ordinary spend.
 *
 * @param amount the value, in [Pence].
 * @param size which numeric style ([TNumSize]).
 * @param showSymbol render the `£` glyph.
 * @param grouped insert thousands separators.
 * @param signed prefix a `+` on positive amounts.
 */
@Composable
fun TNum(
    amount: Pence,
    modifier: Modifier = Modifier,
    size: TNumSize = TNumSize.Medium,
    color: Color = TallyTheme.colors.textHi,
    showSymbol: Boolean = true,
    grouped: Boolean = true,
    signed: Boolean = false,
) {
    val style: TextStyle =
        when (size) {
            TNumSize.Large -> TallyTheme.typography.numericLarge
            TNumSize.Medium -> TallyTheme.typography.numericMedium
            TNumSize.Small -> TallyTheme.typography.numericSmall
        }
    val base = MoneyFormat.format(amount, symbol = showSymbol, grouped = grouped)
    val text = if (signed && amount.isPositive) "+$base" else base
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
    )
}
