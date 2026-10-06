package com.tally.domain.notify

import com.tally.core.money.MoneyFormat
import com.tally.core.money.Pence

/** What the user typed as their share of a bill, checked against the amount paid. */
sealed interface ShareReply {
    /** A usable share, between zero and the amount paid. */
    data class Valid(val share: Pence) : ShareReply

    /** The text is not an amount, or is negative. */
    data object Unreadable : ShareReply

    /** The share is larger than what was paid. */
    data object TooHigh : ShareReply
}

/**
 * Reads the text typed into the capture notification's reply box. It accepts
 * what a person types: `12.50`, `£12.50`, `12`, `.5`. Pure, so it is unit-tested
 * directly. A share above [amount] is rejected, never capped.
 */
fun parseShareReply(
    text: CharSequence?,
    amount: Pence,
): ShareReply {
    val parsed = text?.toString()?.let(MoneyFormat::parse) ?: return ShareReply.Unreadable
    return when {
        parsed.isNegative -> ShareReply.Unreadable
        parsed > amount -> ShareReply.TooHigh
        else -> ShareReply.Valid(parsed)
    }
}
