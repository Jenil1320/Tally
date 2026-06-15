package com.tally.data.repo

import com.tally.core.id.UlidGenerator
import com.tally.core.time.Clock
import com.tally.data.db.dao.TransactionDao
import com.tally.di.IoDispatcher
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.Transaction
import com.tally.domain.repo.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import javax.inject.Inject

/** Room-backed [TransactionRepository]. Maps entities to domain off the IO dispatcher. */
class DefaultTransactionRepository
    @Inject
    constructor(
        private val dao: TransactionDao,
        private val ulidGenerator: UlidGenerator,
        private val clock: Clock,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : TransactionRepository {
        override fun observeAll(): Flow<List<Transaction>> =
            dao.observeAll()
                .map { rows -> rows.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override fun observeBetween(
            from: Instant,
            until: Instant,
        ): Flow<List<Transaction>> =
            dao.observeBetween(from.toEpochMilliseconds(), until.toEpochMilliseconds())
                .map { rows -> rows.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override suspend fun add(draft: NewTransaction): String =
            withContext(ioDispatcher) {
                val id = ulidGenerator.next()
                dao.insert(draft.toEntity(id = id, createdAt = clock.now()))
                id
            }

        override suspend fun softDelete(id: String) =
            withContext(ioDispatcher) {
                dao.softDelete(id, clock.nowMillis())
            }

        override suspend fun countLive(): Int =
            withContext(ioDispatcher) {
                dao.countLive()
            }
    }
