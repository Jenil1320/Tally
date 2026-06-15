package com.tally.domain.model

import com.tally.core.money.Pence
import kotlinx.datetime.Instant

/**
 * A transaction about to be created. The repository mints the ULID id and the
 * `createdAt` stamp, so callers (the Add sheet, the SMS parser, the seeder)
 * supply only the meaningful fields.
 */
data class NewTransaction(
    val amount: Pence,
    val merchant: String,
    val occurredAt: Instant,
    val note: String? = null,
    val categoryId: String? = null,
    val source: TransactionSource = TransactionSource.MANUAL,
)
