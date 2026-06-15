package com.tally.domain.repo

import com.tally.domain.model.Category
import kotlinx.coroutines.flow.Flow

/** The source of truth for categories. */
interface CategoryRepository {
    /** Live categories, in display order. */
    fun observeAll(): Flow<List<Category>>

    /** Count of categories — used by the first-run seeder. */
    suspend fun count(): Int

    /** Inserts categories, ignoring any whose id already exists. */
    suspend fun addAll(categories: List<Category>)
}
