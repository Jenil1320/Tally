package com.tally.di

import android.content.Context
import androidx.room.Room
import com.tally.data.db.TallyDatabase
import com.tally.data.db.dao.CategoryDao
import com.tally.data.db.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the Room database and its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): TallyDatabase = Room.databaseBuilder(context, TallyDatabase::class.java, TallyDatabase.NAME).build()

    @Provides
    fun transactionDao(database: TallyDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun categoryDao(database: TallyDatabase): CategoryDao = database.categoryDao()
}
