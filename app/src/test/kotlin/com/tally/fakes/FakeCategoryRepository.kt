package com.tally.fakes

import com.tally.domain.model.Category
import com.tally.domain.repo.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [CategoryRepository] for ViewModel tests. */
class FakeCategoryRepository(
    initial: List<Category> = emptyList(),
) : CategoryRepository {
    private val items = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<Category>> = items

    override suspend fun count(): Int = items.value.size

    override suspend fun addAll(categories: List<Category>) {
        items.value = items.value + categories
    }
}
