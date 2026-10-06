package com.tally.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema 1 to 2: adds the nullable `myShare` column to `transactions`.
 *
 * The change only adds a column. No row is changed, moved or deleted. Every
 * existing transaction gets `myShare = NULL`, which means "the whole amount is
 * mine", so all old totals stay the same.
 */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `myShare` INTEGER")
        }
    }

/** Every migration, in order. `TallyDatabase` must never fall back to a destructive reset. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
