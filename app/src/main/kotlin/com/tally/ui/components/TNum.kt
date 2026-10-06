package com.tally.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.tally.core.money.MoneyFormat
import com.tally.core.money.Pence
import com.tally.ui.theme.LocalReduceMotion
import com.tally.ui.theme.TallyMotion
import com.tally.ui.theme.TallyTheme

/** Size variants for [TNum], mapped to the mono numeric type scale. */
enum class TNumSize { Hero, Large, Title, Medium, Small }

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
 * [TNumSize.Hero] is the one big figure on a screen: the pound sign and the
 * pence are set smaller and softer, so the whole pounds carry the weight.
 * Set [animate] to count from the old value to the new one when the amount
 * changes ([TallyMotion.COUNT_UP_MS]). The first value shown never animates.
 *
 * @param amount the value, in [Pence].
 * @param size which numeric style ([TNumSize]).
 * @param showSymbol render the `£` glyph.
 * @param grouped insert thousands separators.
 * @param signed prefix a `+` on positive amounts.
 * @param animate count up or down when [amount] changes.
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
    animate: Boolean = false,
) {
    val shown = rememberCountUp(amount, enabled = animate && !LocalReduceMotion.current)
    val typography = TallyTheme.typography
    val style: TextStyle =
        when (size) {
            TNumSize.Hero -> typography.numericHero
            TNumSize.Large -> typography.numericLarge
            TNumSize.Title -> typography.numericTitle
            TNumSize.Medium -> typography.numericMedium
            TNumSize.Small -> typography.numericSmall
        }
    val base = MoneyFormat.format(shown, symbol = showSymbol, grouped = grouped)
    val text = if (signed && shown.isPositive) "+$base" else base
    Text(
        text = if (size == TNumSize.Hero) heroText(text, softColor = TallyTheme.colors.textMid) else AnnotatedString(text),
        modifier = modifier,
        style = style,
        color = color,
        // An amount never breaks across lines.
        maxLines = 1,
        softWrap = false,
    )
}

/** Sets the symbol and the pence of "£1,234.56" smaller and softer than the whole pounds. */
private fun heroText(
    formatted: String,
    softColor: Color,
) = buildAnnotatedString {
    val dot = formatted.lastIndexOf('.')
    val symbolEnd = formatted.indexOfFirst { it.isDigit() }.coerceAtLeast(0)
    val pounds = if (dot >= 0) formatted.substring(symbolEnd, dot) else formatted.substring(symbolEnd)
    val pence = if (dot >= 0) formatted.substring(dot) else ""
    withStyle(SpanStyle(fontSize = 20.sp, color = softColor)) { append(formatted.substring(0, symbolEnd)) }
    append(pounds)
    withStyle(SpanStyle(fontSize = 24.sp, color = softColor)) { append(pence) }
}

/**
 * Follows [target], moving from the value on screen to the new one in a straight
 * line over [TallyMotion.COUNT_UP_MS]. Exact at the end (integer pence).
 */
@Composable
private fun rememberCountUp(
    target: Pence,
    enabled: Boolean,
): Pence {
    var from by remember { mutableLongStateOf(target.value) }
    var to by remember { mutableLongStateOf(target.value) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target.value, enabled) {
        when {
            !enabled -> {
                from = target.value
                to = target.value
                progress.snapTo(1f)
            }
            target.value != to -> {
                from = from + ((to - from) * progress.value).toLong()
                to = target.value
                progress.snapTo(0f)
                progress.animateTo(1f, TallyMotion.countUp())
            }
        }
    }
    return if (enabled) Pence(from + ((to - from) * progress.value).toLong()) else target
}
