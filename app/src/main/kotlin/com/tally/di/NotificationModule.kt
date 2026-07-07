package com.tally.di

import com.tally.data.prefs.DataStoreNotificationIngestPreferences
import com.tally.domain.notify.NotificationIngestPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the notification-capture preference store to its interface. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    @Singleton
    abstract fun notificationIngestPreferences(impl: DataStoreNotificationIngestPreferences): NotificationIngestPreferences
}
