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

    /** Soft delete: tombstone the row instead of removing the spend history. */
    @Query("UPDATE transactions SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(
        id: String,
        deletedAt: Long,
    )
}
