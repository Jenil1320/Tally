package com.tally.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tally.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    /** Live rows, newest first. Tombstoned rows are excluded. */
    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    /** Live rows whose [TransactionEntity.occurredAt] falls in `[from, until)`. */
    @Query(
        "SELECT * FROM transactions " +
            "WHERE deletedAt IS NULL AND occurredAt >= :from AND occurredAt < :until " +
            "ORDER BY occurredAt DESC",
    )
    fun observeBetween(
        from: Long,
        until: Long,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE deletedAt IS NULL")
    suspend fun countLive(): Int

    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    /** Sets only the category of a live row — used by the notification quick-picker. */
    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :id")
    suspend fun updateCategory(
        id: String,
        categoryId: String?,
    )

    /** Sets only the user's share of a row — used by the capture notification's share actions. */
    @Query("UPDATE transactions SET myShare = :myShare WHERE id = :id")
    suspend fun updateShare(
        id: String,
        myShare: Long?,
    )

    /** Count of live rows matching a source + amount + merchant + time — used to de-dup captures. */
    @Query(
        "SELECT COUNT(*) FROM transactions " +
            "WHERE deletedAt IS NULL AND source = :source AND amount = :amount " +
            "AND merchant = :merchant AND occurredAt = :occurredAt",
    )
    suspend fun countMatching(
        source: String,
        amount: Long,
        merchant: String,
        occurredAt: Long,
    ): Int

    /** Category ids of live rows, most-used first, capped at [limit]. */
    @Query(
        "SELECT categoryId FROM transactions " +
            "WHERE deletedAt IS NULL AND categoryId IS NOT NULL " +
            "GROUP BY categoryId ORDER BY COUNT(*) DESC LIMIT :limit",
    )
    suspend fun topCategoryIds(limit: Int): List<String>

    /** Soft delete: tombstone the row instead of removing the spend history. */
    @Query("UPDATE transactions SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(
        id: String,
        deletedAt: Long,
    )
}
