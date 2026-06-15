package com.tally.fakes

import com.tally.domain.category.MerchantRuleStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [MerchantRuleStore] for tests. */
class FakeMerchantRuleStore(
    initial: Map<String, String> = emptyMap(),
) : MerchantRuleStore {
    private val flow = MutableStateFlow(initial)
    override val rules: Flow<Map<String, String>> = flow

    val learned: Map<String, String> get() = flow.value

    override suspend fun learn(
        merchantKey: String,
        categoryId: String,
    ) {
        flow.value = flow.value + (merchantKey to categoryId)
    }
}
