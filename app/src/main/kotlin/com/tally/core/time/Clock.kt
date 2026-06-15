package com.tally.core.time

import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * The time source. Everything that needs "now" depends on this, never on
 * `System.currentTimeMillis()` directly, so tests can pin time with a
 * `FakeClock` (ARCHITECTURE §3). [SystemClock] is the only production impl.
 */
interface Clock {
    /** Milliseconds since the Unix epoch. */
    fun nowMillis(): Long

    /** The current instant, derived from [nowMillis]. */
    fun now(): Instant = Instant.fromEpochMilliseconds(nowMillis())
}

/** Production [Clock] backed by the system wall clock. */
class SystemClock
    @Inject
    constructor() : Clock {
        override fun nowMillis(): Long = System.currentTimeMillis()
    }
