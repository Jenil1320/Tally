package com.tally.data.repo

import com.tally.data.db.dao.CategoryDao
import com.tally.di.IoDispatcher
import com.tally.domain.model.Category
import com.tally.domain.repo.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Room-backed [CategoryRepository]. */
class DefaultCategoryRepository
    @Inject
    constructor(
        private val dao: CategoryDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : CategoryRepository {
        override fun observeAll(): Flow<List<Category>> =
            dao.observeAll()
                .map { rows -> rows.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override suspend fun count(): Int =
            withContext(ioDispatcher) {
                dao.count()
            }

        override suspend fun addAll(categories: List<Category>) =
            withContext(ioDispatcher) {
                dao.insertAll(categories.map { it.toEntity() })
            }
    }
