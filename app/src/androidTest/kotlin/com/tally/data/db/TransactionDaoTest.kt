package com.tally.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tally.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DAO round-trip against a real in-memory Room database. Instrumented (needs a
 * device/emulator); not part of the JVM unit-test path.
 */
@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var database: TallyDatabase
    private lateinit var dao: com.tally.data.db.dao.TransactionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
        dao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun row(
        id: String,
        occurredAt: Long,
        deletedAt: Long? = null,
    ) = TransactionEntity(
        id = id,
        amount = 485,
        merchant = "Coffee",
        note = null,
        categoryId = null,
        occurredAt = occurredAt,
        createdAt = occurredAt,
        source = "MANUAL",
        smsRaw = null,
        deletedAt = deletedAt,
    )

    @Test
    fun insertsAndObservesNewestFirst() =
        runTest {
            dao.insert(row("a", occurredAt = 100))
            dao.insert(row("b", occurredAt = 200))
            val all = dao.observeAll().first()
            assertThat(all.map { it.id }).containsExactly("b", "a").inOrder()
        }

    @Test
    fun softDeleteHidesFromLiveQueries() =
        runTest {
            dao.insert(row("a", occurredAt = 100))
            dao.softDelete("a", deletedAt = 999)
            assertThat(dao.observeAll().first()).isEmpty()
            assertThat(dao.countLive()).isEqualTo(0)
            assertThat(dao.findById("a")?.deletedAt).isEqualTo(999)
        }
}
