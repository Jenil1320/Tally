package com.tally.core.id

import com.tally.core.time.Clock
import kotlin.random.Random

/**
 * Generates ULIDs — 26-char Crockford base32 identifiers used as the primary key
 * on every table (ARCHITECTURE §3). A ULID sorts lexicographically by creation
 * time, so "newest first" queries need no separate timestamp index, and it is
 * monotonic *within* a millisecond so two rows created in the same tick still
 * order by insertion.
 */
fun interface UlidGenerator {
    /** A new ULID. Always 26 chars from the Crockford alphabet. */
    fun next(): String
}

private const val CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
private const val TIME_CHARS = 10
private const val RANDOM_CHARS = 16

/**
 * The production [UlidGenerator]. Thread-safe and monotonic: within one
 * millisecond it increments the random component rather than re-rolling it, so
 * IDs minted in the same tick remain strictly increasing.
 *
 * Constructed via Hilt with a [Clock] and a crypto-strong [Random]; tests inject
 * a `FakeClock` and a seeded `Random` for deterministic output.
 */
class MonotonicUlidGenerator(
    private val clock: Clock,
    private val random: Random,
) : UlidGenerator {
    private val lock = Any()
    private var lastTime = -1L
    private val randomComponent = IntArray(RANDOM_CHARS)

    override fun next(): String =
        synchronized(lock) {
            val time = clock.nowMillis()
            require(time >= 0) { "Clock returned a negative time: $time" }
            if (time == lastTime) {
                incrementRandom()
            } else {
                lastTime = time
                for (i in randomComponent.indices) {
                    randomComponent[i] = random.nextInt(CROCKFORD.length)
                }
            }
            buildString {
                append(encodeTime(time))
                for (value in randomComponent) append(CROCKFORD[value])
            }
        }

    private fun incrementRandom() {
        for (i in RANDOM_CHARS - 1 downTo 0) {
            if (randomComponent[i] < CROCKFORD.length - 1) {
                randomComponent[i]++
                return
            }
            randomComponent[i] = 0
        }
        // Overflow of all 80 random bits inside one millisecond is not reachable
        // in practice; wrapping to zero keeps the contract (still 26 valid chars).
    }

    private fun encodeTime(time: Long): String {
        val chars = CharArray(TIME_CHARS)
        var remaining = time
        for (i in TIME_CHARS - 1 downTo 0) {
            chars[i] = CROCKFORD[(remaining % CROCKFORD.length).toInt()]
            remaining /= CROCKFORD.length
        }
        return String(chars)
    }
}
