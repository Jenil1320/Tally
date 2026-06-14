package com.tally.core.money

/**
 * GBP formatting and parsing for [Pence]. Single-currency by constitution, so
 * there is no `Locale`/`Currency` machinery — the rules are hard-coded and
 * tested: `£`, comma thousands-separators, a leading minus for negatives,
 * always exactly two decimal places.
 *
 * All arithmetic here is integer-only (no `Float`/`Double`), so the rendered
 * string is an exact transcription of the stored pence.
 */
object MoneyFormat {
    private const val PENCE_PER_POUND = 100L

    /**
     * Renders [amount] as a GBP string, e.g. `£1,234.56` or `-£5.00`.
     *
     * @param symbol prepend the `£` glyph. Off for bare-number contexts where
     *   the symbol is drawn separately (the keypad preview in M2).
     * @param grouped insert `,` thousands separators.
     */
    fun format(
        amount: Pence,
        symbol: Boolean = true,
        grouped: Boolean = true,
    ): String {
        val abs = amount.value.let { if (it < 0) -it else it }
        val pounds = abs / PENCE_PER_POUND
        val pennies = abs % PENCE_PER_POUND
        val poundsText = if (grouped) groupThousands(pounds) else pounds.toString()
        return buildString {
            if (amount.value < 0) append('-')
            if (symbol) append('£')
            append(poundsText)
            append('.')
            if (pennies < 10) append('0')
            append(pennies)
        }
    }

    /**
     * Parses user input into [Pence], or `null` if it isn't a valid GBP amount.
     *
     * Tolerant of what a person actually types: a leading `£`, surrounding
     * whitespace, comma grouping, a leading `+`/`-`, a bare `.50`, and zero,
     * one, or two decimal places. Three+ decimals, letters, or multiple dots
     * are rejected rather than rounded — the keypad never produces them, and
     * silently rounding money is a footgun.
     */
    fun parse(input: String): Pence? {
        var text = input.trim()
        if (text.isEmpty()) return null

        var negative = false
        when (text.firstOrNull()) {
            '-' -> {
                negative = true
                text = text.drop(1)
            }
            '+' -> text = text.drop(1)
        }
        text = text.removePrefix("£").trim().replace(",", "")
        if (text.isEmpty()) return null

        val dot = text.indexOf('.')
        val wholePart: String
        val fracPart: String
        if (dot < 0) {
            wholePart = text
            fracPart = ""
        } else {
            wholePart = text.substring(0, dot)
            fracPart = text.substring(dot + 1)
            if (fracPart.contains('.')) return null
        }

        // Allow ".50" (empty whole) but not an entirely empty number.
        if (wholePart.isEmpty() && fracPart.isEmpty()) return null
        if (wholePart.any { !it.isDigit() }) return null
        if (fracPart.any { !it.isDigit() }) return null
        if (fracPart.length > 2) return null

        val pounds = if (wholePart.isEmpty()) 0L else wholePart.toLongOrNull() ?: return null
        val pennies = fracPart.padEnd(2, '0').toLongOrNull() ?: return null
        val magnitude = pounds * PENCE_PER_POUND + pennies
        return Pence(if (negative) -magnitude else magnitude)
    }

    private fun groupThousands(n: Long): String {
        val digits = n.toString()
        if (digits.length <= 3) return digits
        return buildString {
            val firstGroup = digits.length % 3
            if (firstGroup > 0) {
                append(digits, 0, firstGroup)
                if (digits.length > firstGroup) append(',')
            }
            var i = firstGroup
            while (i < digits.length) {
                append(digits, i, i + 3)
                if (i + 3 < digits.length) append(',')
                i += 3
            }
        }
    }
}

/** Ergonomic shorthand for [MoneyFormat.format]. */
fun Pence.format(
    symbol: Boolean = true,
    grouped: Boolean = true,
): String = MoneyFormat.format(this, symbol, grouped)
