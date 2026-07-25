package com.tally.domain.insight

import com.tally.core.money.Pence
import kotlinx.coroutines.flow.Flow

/**
 * Persists the monthly spending limit the user set per category. Limits are the
 * user's own pre-commitment, so nothing here is inferred or defaulted — a
 * category with no entry simply has no limit.
 */
interface CategoryLimitStore {
    /** Live limits: categoryId → monthly limit. */
    val limits: Flow<Map<String, Pence>>

    /** Sets [categoryId]'s monthly limit, or clears it when [limit] is null or not positive. */
    suspend fun setLimit(
        categoryId: String,
        limit: Pence?,
    )
}
