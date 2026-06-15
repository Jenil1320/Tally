package com.tally.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tally.data.db.dao.CategoryDao
import com.tally.data.db.dao.TransactionDao
import com.tally.data.db.entity.CategoryEntity
import com.tally.data.db.entity.TransactionEntity

/**
 * The Tally Room database, schema version 1.
 *
 * `exportSchema = true` writes the schema JSON to `/schemas` (configured in
 * `app/build.gradle.kts`); every future version ships a migration plus a test
 * that walks `1 -> currentVersion` (ARCHITECTURE §6). A `fallbackToDestructive*`
 * is never used on a release build — that is asserted by a test in M6.
 */
@Database(
    entities = [TransactionEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TallyDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        const val NAME = "tally.db"
    }
}
