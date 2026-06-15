package com.tally.domain.repo

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

    /** Creates a transaction and returns its new ULID. */
    suspend fun add(draft: NewTransaction): String

    /** Soft-deletes (tombstones) a transaction. */
    suspend fun softDelete(id: String)

    /** Count of live transactions — used by the first-run seeder. */
    suspend fun countLive(): Int
}
