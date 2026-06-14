package com.tally.core.money

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PenceTest {
    // --- Formatting --------------------------------------------------------

    @Test fun `format renders two decimal places`() {
        assertThat(Pence(0).format()).isEqualTo("£0.00")
        assertThat(Pence(5).format()).isEqualTo("£0.05")
        assertThat(Pence(50).format()).isEqualTo("£0.50")
        assertThat(Pence(1234).format()).isEqualTo("£12.34")
    }

    @Test fun `format groups thousands`() {
        assertThat(Pence(123456).format()).isEqualTo("£1,234.56")
        assertThat(Pence(100_000_000).format()).isEqualTo("£1,000,000.00")
        assertThat(Pence(99999).format()).isEqualTo("£999.99")
    }

    @Test fun `format prefixes a minus for negatives`() {
        assertThat(Pence(-500).format()).isEqualTo("-£5.00")
        assertThat(Pence(-123456).format()).isEqualTo("-£1,234.56")
    }

    @Test fun `format can drop the symbol and grouping`() {
        assertThat(Pence(123456).format(symbol = false)).isEqualTo("1,234.56")
        assertThat(Pence(123456).format(grouped = false)).isEqualTo("£1234.56")
        assertThat(Pence(123456).format(symbol = false, grouped = false)).isEqualTo("1234.56")
    }

    // --- Parsing -----------------------------------------------------------

    @Test fun `parse accepts the shapes a person types`() {
        assertThat(MoneyFormat.parse("12.34")).isEqualTo(Pence(1234))
        assertThat(MoneyFormat.parse("£12.34")).isEqualTo(Pence(1234))
        assertThat(MoneyFormat.parse("1,234.56")).isEqualTo(Pence(123456))
        assertThat(MoneyFormat.parse("12")).isEqualTo(Pence(1200))
        assertThat(MoneyFormat.parse(".5")).isEqualTo(Pence(50))
        assertThat(MoneyFormat.parse("12.3")).isEqualTo(Pence(1230))
        assertThat(MoneyFormat.parse("-5")).isEqualTo(Pence(-500))
        assertThat(MoneyFormat.parse("  £ 1,000.00 ")).isEqualTo(Pence(100_000))
    }

    @Test fun `parse rejects nonsense rather than rounding`() {
        assertThat(MoneyFormat.parse("")).isNull()
        assertThat(MoneyFormat.parse("£")).isNull()
        assertThat(MoneyFormat.parse("abc")).isNull()
        assertThat(MoneyFormat.parse("1.234")).isNull()
        assertThat(MoneyFormat.parse("1.2.3")).isNull()
        assertThat(MoneyFormat.parse("12p")).isNull()
    }

    @Test fun `parse and format round-trip`() {
        listOf(0L, 5L, 1234L, 123456L, -500L).forEach { raw ->
            val formatted = Pence(raw).format()
            assertThat(MoneyFormat.parse(formatted)).isEqualTo(Pence(raw))
        }
    }

    // --- Arithmetic --------------------------------------------------------

    @Test fun `arithmetic operators work`() {
        assertThat(Pence(1000) + Pence(234)).isEqualTo(Pence(1234))
        assertThat(Pence(1000) - Pence(234)).isEqualTo(Pence(766))
        assertThat(-Pence(500)).isEqualTo(Pence(-500))
        assertThat(Pence(250) * 3).isEqualTo(Pence(750))
        assertThat(Pence(250) * 3L).isEqualTo(Pence(750))
    }

    @Test fun `comparison and predicates`() {
        assertThat(Pence(100) > Pence(99)).isTrue()
        assertThat(Pence(0).isZero).isTrue()
        assertThat(Pence(-1).isNegative).isTrue()
        assertThat(Pence(1).isPositive).isTrue()
        assertThat(Pence(-1234).absoluteValue).isEqualTo(Pence(1234))
    }

    @Test fun `ofPounds composes pounds and pennies`() {
        assertThat(Pence.ofPounds(12, 34)).isEqualTo(Pence(1234))
        assertThat(Pence.ofPounds(0, 5)).isEqualTo(Pence(5))
        assertThat(Pence.ofPounds(-5)).isEqualTo(Pence(-500))
        assertThrows(IllegalArgumentException::class.java) { Pence.ofPounds(1, 100) }
    }

    // --- Splitting ---------------------------------------------------------

    @Test fun `splitEvenly reconciles to the original`() {
        val parts = Pence(1000).splitEvenly(3)
        assertThat(parts).containsExactly(Pence(334), Pence(333), Pence(333)).inOrder()
        assertThat(parts.fold(Pence.ZERO) { a, b -> a + b }).isEqualTo(Pence(1000))
    }

    @Test fun `splitEvenly spreads remainder pennies`() {
        assertThat(Pence(10).splitEvenly(4)).containsExactly(Pence(3), Pence(3), Pence(2), Pence(2)).inOrder()
    }

    @Test fun `splitEvenly handles negatives and single share`() {
        val neg = Pence(-1000).splitEvenly(3)
        assertThat(neg.fold(Pence.ZERO) { a, b -> a + b }).isEqualTo(Pence(-1000))
        assertThat(Pence(999).splitEvenly(1)).containsExactly(Pence(999))
        assertThrows(IllegalArgumentException::class.java) { Pence(100).splitEvenly(0) }
    }
}
