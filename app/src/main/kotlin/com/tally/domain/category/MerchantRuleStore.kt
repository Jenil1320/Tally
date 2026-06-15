package com.tally.domain.category

import kotlinx.coroutines.flow.Flow

/**
 * Persists merchant→category rules the app has *learned* from the user's
 * choices. Keyed by a normalised merchant string (see `normaliseMerchant`).
 * These take precedence over the bundled rules.
 */
interface MerchantRuleStore {
    /** Learned rules: normalised merchant → categoryId. */
    val rules: Flow<Map<String, String>>

    /** Remember that [merchantKey] (already normalised) maps to [categoryId]. */
    suspend fun learn(
        merchantKey: String,
        categoryId: String,
    )
}
