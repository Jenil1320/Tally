package com.tally.di

import com.tally.data.repo.DefaultCategoryRepository
import com.tally.data.repo.DefaultTransactionRepository
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds repository implementations to their domain interfaces. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun transactionRepository(impl: DefaultTransactionRepository): TransactionRepository

    @Binds
    @Singleton
    abstract fun categoryRepository(impl: DefaultCategoryRepository): CategoryRepository
}
