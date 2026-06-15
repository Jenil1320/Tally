package com.tally.di

import com.tally.core.id.MonotonicUlidGenerator
import com.tally.core.id.UlidGenerator
import com.tally.core.time.Clock
import com.tally.core.time.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import javax.inject.Singleton
import kotlin.random.asKotlinRandom

/** Core singletons: the time source and the ULID generator (ARCHITECTURE §4). */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun clock(): Clock = SystemClock()

    @Provides
    @Singleton
    fun ulidGenerator(clock: Clock): UlidGenerator = MonotonicUlidGenerator(clock, SecureRandom().asKotlinRandom())
}
