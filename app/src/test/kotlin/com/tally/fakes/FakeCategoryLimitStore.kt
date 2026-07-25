package com.tally.fakes

import com.tally.core.money.Pence
import com.tally.domain.insight.CategoryLimitStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [CategoryLimitStore] for tests. */
class FakeCategoryLimitStore(
    initial: Map<String, Pence> = emptyMap(),
) : CategoryLimitStore {
    private val flow = MutableStateFlow(initial)
    override val limits: Flow<Map<String, Pence>> = flow

    override suspend fun setLimit(
        categoryId: String,
        limit: Pence?,
    ) {
        flow.value =
            if (limit == null || limit.value <= 0L) {
                flow.value - categoryId
            } else {
                flow.value + (categoryId to limit)
            }
    }
}
