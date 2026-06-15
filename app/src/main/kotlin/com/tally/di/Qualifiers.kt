package com.tally.di

import javax.inject.Qualifier

/** Marks the IO-bound dispatcher (DB, parsing). See ARCHITECTURE §5. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Marks the CPU-bound dispatcher (analytics formulas). See ARCHITECTURE §5. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
