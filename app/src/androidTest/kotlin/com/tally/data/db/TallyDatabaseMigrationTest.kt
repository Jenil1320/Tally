package com.tally.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks the schema from version 1 to the current version (ARCHITECTURE §6).
 *
 * With only v1 today this asserts that the exported v1 schema creates cleanly.
 * Each future release adds its `Migration` to `TallyDatabase` and a
 * `runMigrationsAndValidate(TEST_DB, newVersion, true, migration)` call here, so
 * the chain is always tested end to end.
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

    private companion object {
        const val TEST_DB = "tally-migration-test"
    }
}
