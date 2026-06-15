package com.tally.core.time

/** A [Clock] with controllable time, for deterministic tests. */
class FakeClock(private var millis: Long = 0L) : Clock {
    override fun nowMillis(): Long = millis

    fun setTo(millis: Long) {
        this.millis = millis
    }

    fun advanceBy(deltaMillis: Long) {
        millis += deltaMillis
    }
}
