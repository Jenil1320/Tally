package com.tally.domain.model

import com.tally.core.money.Pence
import kotlinx.datetime.Instant

/**
 * A single recorded transaction, in domain terms (no Room/Android types).
 *
 * [amount] is signed in spend terms: a purchase is **positive** (money out), a
 * refund/credit is **negative** (money back). "Spent today/this month" is the
 * sum of amounts, so refunds correctly reduce the total. Income tracking is out
 * of scope (CONTRIBUTING §16), so there is no separate income concept.
 *
 * @property occurredAt when the spend happened (user-editable).
 * @property createdAt when the row was written (immutable; ULIDs also encode it).
 */
data class Transaction(
    val id: String,
    val amount: Pence,
    val merchant: String,
    val note: String?,
    val categoryId: String?,
    val occurredAt: Instant,
    val createdAt: Instant,
    val source: TransactionSource,
)

/** How a transaction entered Tally. */
enum class TransactionSource {
    /** Typed in via the Add sheet. */
    MANUAL,

    /** Parsed from a bank SMS the user granted access to (M4). */
    SMS,

    /** Brought in by the CSV importer (M4). */
    IMPORT,
}
