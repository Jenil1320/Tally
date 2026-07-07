package com.tally.data.repo

import com.tally.data.db.dao.TransactionDao
import com.tally.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [TransactionDao] for repository unit tests. */
class FakeTransactionDao : TransactionDao {
    private val rows = MutableStateFlow<List<TransactionEntity>>(emptyList())

    private fun live(list: List<TransactionEntity>) = list.filter { it.deletedAt == null }.sortedByDescending { it.occurredAt }

    override fun observeAll(): Flow<List<TransactionEntity>> = rows.map { live(it) }

    override fun observeBetween(
        from: Long,
        until: Long,
    ): Flow<List<TransactionEntity>> = rows.map { list -> live(list).filter { it.occurredAt in from until until } }

    override suspend fun findById(id: String): TransactionEntity? = rows.value.find { it.id == id }

    override suspend fun countLive(): Int = rows.value.count { it.deletedAt == null }

    override suspend fun insert(transaction: TransactionEntity) {
        rows.value = rows.value + transaction
    }

    override suspend fun update(transaction: TransactionEntity) {
        rows.value = rows.value.map { if (it.id == transaction.id) transaction else it }
    }

    override suspend fun updateCategory(
        id: String,
        categoryId: String?,
    ) {
        rows.value = rows.value.map { if (it.id == id) it.copy(categoryId = categoryId) else it }
    }

    override suspend fun topCategoryIds(limit: Int): List<String> =
        rows.value
            .filter { it.deletedAt == null }
            .mapNotNull { it.categoryId }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }

    override suspend fun softDelete(
        id: String,
        deletedAt: Long,
    ) {
        rows.value = rows.value.map { if (it.id == id) it.copy(deletedAt = deletedAt) else it }
    }
}
