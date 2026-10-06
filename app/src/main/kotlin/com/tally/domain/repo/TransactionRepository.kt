package com.tally.domain.repo

import com.tally.core.money.Pence
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * The source of truth for transactions (ARCHITECTURE §2). Screens subscribe to
 * the [Flow]s and never cache derived state the repo couldn't recompute.
 */
interface TransactionRepository {
    /** Live transactions, newest first. */
    fun observeAll(): Flow<List<Transaction>>

    /** Live transactions with `occurredAt` in `[from, until)`. */
    fun observeBetween(
        from: Instant,
        until: Instant,
    ): Flow<List<Transaction>>

    /** A single transaction by id, or null if it doesn't exist. */
    suspend fun findById(id: String): Transaction?

    /** Creates a transaction and returns its new ULID. */
    suspend fun add(draft: NewTransaction): String

    /**
     * Applies [draft]'s editable fields to the transaction [id], preserving its
     * id, createdAt, smsRaw, and soft-delete state. No-op if [id] is unknown.
     */
    suspend fun update(
        id: String,
        draft: NewTransaction,
    )

    /** Sets only the category of a transaction (the notification quick-picker). */
    suspend fun updateCategory(
        id: String,
        categoryId: String?,
    )

    /**
     * Sets only the user's share of a transaction (the capture notification's
     * share actions). A share outside zero to the amount paid, or an unknown
     * [id], is ignored. Null clears the share.
     */
    suspend fun updateShare(
        id: String,
        myShare: Pence?,
    )

    /** The most-used category ids across live rows, most-frequent first, up to [limit]. */
    suspend fun topCategoryIds(limit: Int): List<String>

    /**
     * True if a notification-sourced transaction with this exact amount, merchant,
     * and time already exists — the de-dup guard so rescanning the notification
     * shade can't log the same payment twice.
     */
    suspend fun notificationTransactionExists(
        amount: Pence,
        merchant: String,
        occurredAt: Instant,
    ): Boolean

    /** Soft-deletes (tombstones) a transaction. */
    suspend fun softDelete(id: String)

    /** Count of live transactions — used by the first-run seeder. */
    suspend fun countLive(): Int
}
