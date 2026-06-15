package com.tally.di

import com.tally.data.prefs.DataStoreReminderPreferences
import com.tally.domain.reminder.ReminderPreferences
import com.tally.domain.reminder.ReminderScheduler
import com.tally.work.WorkManagerReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the reminder preference store and scheduler to their interfaces. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    @Binds
    @Singleton
    abstract fun reminderPreferences(impl: DataStoreReminderPreferences): ReminderPreferences

    @Binds
    @Singleton
    abstract fun reminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}
