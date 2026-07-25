package com.tally.domain.insight

import com.tally.core.money.Pence
import com.tally.domain.model.Transaction

/**
 * One merchant's spend over a period.
 *
 * @property merchant the display name (the most common raw spelling seen).
 * @property visits how many transactions — the "often" half of small-and-often.
 */
data class MerchantTotal(
    val key: String,
    val merchant: String,
    val total: Pence,
    val visits: Int,
)

/**
 * Tokens that carry no identity and only split one merchant into several
 * (a store number is already dropped by the letters-only filter).
 */
private val NOISE_TOKENS =
    setOf("LTD", "LIMITED", "PLC", "UK", "GB", "COM", "CO", "WWW", "THE", "STORES", "STORE", "SHOP", "PAYMENT", "PAYPAL")

/**
 * A grouping key for a raw merchant string from a payment notification:
 * uppercase, letters only, noise tokens dropped, first two tokens kept — so
 * "TESCO STORES 6530" and "Tesco" land together.
 *
 * Deliberately separate from `normaliseMerchant` in `domain/category`, which
 * must keep exact-match semantics for learned category rules. Pure.
 */
fun merchantKey(raw: String): String {
    val tokens =
        raw.uppercase()
            .map { if (it.isLetter() || it == ' ') it else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.isNotBlank() && it !in NOISE_TOKENS }
    if (tokens.isEmpty()) return raw.trim().uppercase()
    return tokens.take(2).joinToString(" ")
}

/**
 * Totals [transactions] by merchant, largest spend first, capped at [limit].
 *
 * Surfaces the "small and often" blind spot the category donut hides: many small
 * purchases at one merchant read as a rounding error per transaction but can
 * outweigh a single large one. Pure and unit-tested; integer pence only.
 */
fun merchantTotals(
    transactions: List<Transaction>,
    limit: Int = 5,
): List<MerchantTotal> {
    if (transactions.isEmpty() || limit <= 0) return emptyList()
    return transactions
        .groupBy { merchantKey(it.merchant) }
        .map { (key, group) ->
            // Display the most common raw spelling; ties go to the shortest.
            val display =
                group.groupingBy { it.merchant.trim() }
                    .eachCount()
                    .entries
                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.length })
                    .first()
                    .key
            MerchantTotal(
                key = key,
                merchant = display,
                total = Pence(group.sumOf { it.amount.value }),
                visits = group.size,
            )
        }
        .sortedByDescending { it.total.value }
        .take(limit)
}
