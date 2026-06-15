package com.tally.di

import com.tally.data.category.DataStoreMerchantRuleStore
import com.tally.domain.category.MerchantRuleStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the learned merchant-rule store. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CategoriserModule {
    @Binds
    @Singleton
    abstract fun merchantRuleStore(impl: DataStoreMerchantRuleStore): MerchantRuleStore
}
