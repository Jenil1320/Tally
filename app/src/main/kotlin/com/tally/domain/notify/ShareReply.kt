package com.tally.domain.notify

import com.tally.core.money.MoneyFormat
import com.tally.core.money.Pence

/** What the user typed as their share of a bill, checked against the amount paid. */
sealed interface ShareReply {
    /** A usable share, between zero and the amount paid. */
    data class Valid(val share: Pence) : ShareReply

    /** The text is not an amount or a percent, or is negative. */
    data object Unreadable : ShareReply

    /** The share is larger than what was paid. */
    data object TooHigh : ShareReply
}

private const val PERCENT_SCALE = 10_000L // hundredths of a percent in 100%

/**
 * Reads the text typed into the capture notification's reply box. An amount in
 * pounds is read as typed: `12.50`, `£12.50`, `12`, `.5`. A trailing `%` makes it
 * a percent of the amount paid: `25%` is a quarter, `12.5%` is an eighth. A
 * percent rounds to the nearest penny. Pure, so it is unit-tested directly. A
 * share above [amount] is rejected, never capped.
 */
fun parseShareReply(
    text: CharSequence?,
    amount: Pence,
): ShareReply {
    val raw = text?.toString()?.trim() ?: return ShareReply.Unreadable
    if (raw.endsWith("%")) {
        // "12.5" parses as 12.50, which is 1,250 hundredths of a percent.
        val hundredths = MoneyFormat.parse(raw.dropLast(1))?.value ?: return ShareReply.Unreadable
        return when {
            hundredths < 0 -> ShareReply.Unreadable
            hundredths > PERCENT_SCALE -> ShareReply.TooHigh
            else -> ShareReply.Valid(Pence((amount.value * hundredths + PERCENT_SCALE / 2) / PERCENT_SCALE))
        }
    }
    val parsed = MoneyFormat.parse(raw) ?: return ShareReply.Unreadable
    return when {
        parsed.isNegative -> ShareReply.Unreadable
        parsed > amount -> ShareReply.TooHigh
        else -> ShareReply.Valid(parsed)
    }
}
