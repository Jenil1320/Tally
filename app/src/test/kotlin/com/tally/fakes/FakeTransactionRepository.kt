package com.tally.fakes

import com.tally.core.time.Clock
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.Transaction
import com.tally.domain.repo.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/** In-memory [TransactionRepository] for ViewModel tests. */
class FakeTransactionRepository(
    private val clock: Clock,
) : TransactionRepository {
    private val items = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextId = 0

    override fun observeAll(): Flow<List<Transaction>> = items.map { list -> list.sortedByDescending { it.occurredAt } }

    override fun observeBetween(
        from: Instant,
        until: Instant,
    ): Flow<List<Transaction>> =
        items.map { list ->
            list.filter { it.occurredAt >= from && it.occurredAt < until }
                .sortedByDescending { it.occurredAt }
        }

    override suspend fun findById(id: String): Transaction? = items.value.find { it.id == id }

    override suspend fun update(
        id: String,
        draft: NewTransaction,
    ) {
        items.value =
            items.value.map { existing ->
                if (existing.id == id) {
                    existing.copy(
                        amount = draft.amount,
                        merchant = draft.merchant,
                        note = draft.note,
                        categoryId = draft.categoryId,
                        occurredAt = draft.occurredAt,
                        source = draft.source,
                    )
                } else {
                    existing
                }
            }
    }

    override suspend fun add(draft: NewTransaction): String {
        val id = "tx${nextId++}"
        items.value =
            items.value +
            Transaction(
                id = id,
                amount = draft.amount,
                merchant = draft.merchant,
                note = draft.note,
                categoryId = draft.categoryId,
                occurredAt = draft.occurredAt,
                createdAt = clock.now(),
                source = draft.source,
            )
        return id
    }

    override suspend fun softDelete(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun countLive(): Int = items.value.size
}
