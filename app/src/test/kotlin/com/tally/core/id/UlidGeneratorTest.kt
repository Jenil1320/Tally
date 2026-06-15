package com.tally.core.id

import com.google.common.truth.Truth.assertThat
import com.tally.core.time.FakeClock
import org.junit.jupiter.api.Test
import kotlin.random.Random

class UlidGeneratorTest {
    private val crockford = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toSet()

    @Test fun `produces 26 Crockford-base32 chars`() {
        val ulid = MonotonicUlidGenerator(FakeClock(1_700_000_000_000), Random(42)).next()
        assertThat(ulid).hasLength(26)
        assertThat(ulid.all { it in crockford }).isTrue()
    }

    @Test fun `is monotonic within the same millisecond`() {
        val generator = MonotonicUlidGenerator(FakeClock(1_700_000_000_000), Random(7))
        val a = generator.next()
        val b = generator.next()
        val c = generator.next()
        assertThat(a < b).isTrue()
        assertThat(b < c).isTrue()
    }

    @Test fun `a later timestamp sorts after an earlier one`() {
        val clock = FakeClock(1_000)
        val generator = MonotonicUlidGenerator(clock, Random(7))
        val early = generator.next()
        clock.advanceBy(1_000)
        val late = generator.next()
        assertThat(early < late).isTrue()
    }

    @Test fun `the time component is identical for the same instant`() {
        val clock = FakeClock(123_456_789)
        val first = MonotonicUlidGenerator(clock, Random(1)).next()
        val second = MonotonicUlidGenerator(clock, Random(2)).next()
        assertThat(first.take(10)).isEqualTo(second.take(10))
    }
}
