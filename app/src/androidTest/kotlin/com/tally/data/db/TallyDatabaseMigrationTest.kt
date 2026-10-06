package com.tally.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks the schema from version 1 to the current version (ARCHITECTURE §6).
 *
 * Each release adds its `Migration` to `ALL_MIGRATIONS` and a test here, so the
 * chain is always tested end to end. The 1 to 2 test is the data-safety check:
 * it writes rows with the old schema, migrates, and proves every row survived
 * unchanged with `myShare` empty.
 *
 * Instrumented (needs a device/emulator); not run on the JVM unit-test path.
 */
@RunWith(AndroidJUnit4::class)
class TallyDatabaseMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            TallyDatabase::class.java,
        )

    @Test
    fun createsSchemaAtVersion1() {
        helper.createDatabase(TEST_DB, 1).close()
    }

    @Test
    fun migration1To2KeepsEveryRowAndLeavesMyShareEmpty() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO categories (id, name, colorIndex, isSystem, sortOrder) VALUES ('c1', 'Eating out', 1, 1, 1)")
            execSQL(
                "INSERT INTO transactions (id, amount, merchant, note, categoryId, occurredAt, createdAt, source, smsRaw, deletedAt) " +
                    "VALUES ('t1', 3840, 'Dishoom', 'dinner', 'c1', 1000, 1000, 'MANUAL', NULL, NULL)",
            )
            execSQL(
                "INSERT INTO transactions (id, amount, merchant, note, categoryId, occurredAt, createdAt, source, smsRaw, deletedAt) " +
                    "VALUES ('t2', 485, 'Pret', NULL, NULL, 2000, 2000, 'NOTIFICATION', NULL, 5000)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT id, amount, merchant, note, categoryId, occurredAt, source, deletedAt, myShare FROM transactions ORDER BY id")
            .use { cursor ->
                assertThat(cursor.count).isEqualTo(2)

                cursor.moveToFirst()
                assertThat(cursor.getString(0)).isEqualTo("t1")
                assertThat(cursor.getLong(1)).isEqualTo(3840L)
                assertThat(cursor.getString(2)).isEqualTo("Dishoom")
                assertThat(cursor.getString(3)).isEqualTo("dinner")
                assertThat(cursor.getString(4)).isEqualTo("c1")
                assertThat(cursor.getLong(5)).isEqualTo(1000L)
                assertThat(cursor.getString(6)).isEqualTo("MANUAL")
                assertThat(cursor.isNull(7)).isTrue()
                assertThat(cursor.isNull(8)).isTrue()

                cursor.moveToNext()
                assertThat(cursor.getString(0)).isEqualTo("t2")
                assertThat(cursor.getLong(1)).isEqualTo(485L)
                assertThat(cursor.getString(6)).isEqualTo("NOTIFICATION")
                assertThat(cursor.getLong(7)).isEqualTo(5000L)
                assertThat(cursor.isNull(8)).isTrue()
            }
        db.close()
    }

    private companion object {
        const val TEST_DB = "tally-migration-test"
    }
}
