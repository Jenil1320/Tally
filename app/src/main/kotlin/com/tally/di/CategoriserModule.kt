package com.tally.di

import com.tally.data.category.DataStoreMerchantRuleStore
import com.tally.data.prefs.DataStoreCategoryLimitStore
import com.tally.domain.category.MerchantRuleStore
import com.tally.domain.insight.CategoryLimitStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the learned merchant-rule store and the category-limit store. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CategoriserModule {
    @Binds
    @Singleton
    abstract fun merchantRuleStore(impl: DataStoreMerchantRuleStore): MerchantRuleStore

    @Binds
    @Singleton
    abstract fun categoryLimitStore(impl: DataStoreCategoryLimitStore): CategoryLimitStore
}
