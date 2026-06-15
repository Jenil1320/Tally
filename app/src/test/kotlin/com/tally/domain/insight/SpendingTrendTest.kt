package com.tally.domain.insight

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.domain.model.Transaction
import com.tally.domain.model.TransactionSource
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

class SpendingTrendTest {
    private val utc = TimeZone.UTC

    private fun tx(
        amount: Long,
        iso: String,
    ) = Transaction(
        id = "x",
        amount = Pence(amount),
        merchant = "m",
        note = null,
        categoryId = null,
        occurredAt = Instant.parse(iso),
        createdAt = Instant.parse(iso),
        source = TransactionSource.MANUAL,
    )

    @Test fun `lengthOfMonth handles month lengths and leap years`() {
        assertThat(LocalDate(2026, 6, 1).lengthOfMonth()).isEqualTo(30)
        assertThat(LocalDate(2026, 1, 1).lengthOfMonth()).isEqualTo(31)
        assertThat(LocalDate(2026, 2, 1).lengthOfMonth()).isEqualTo(28)
        assertThat(LocalDate(2024, 2, 1).lengthOfMonth()).isEqualTo(29)
    }

    @Test fun `dailyTotals buckets by day of month`() {
        val daily =
            dailyTotals(
                listOf(tx(2000, "2026-06-01T09:00:00Z"), tx(1000, "2026-06-15T09:00:00Z"), tx(500, "2026-06-15T18:00:00Z")),
                utc,
                LocalDate(2026, 6, 1),
            )
        assertThat(daily).hasSize(30)
        assertThat(daily[0]).isEqualTo(Pence(2000))
        assertThat(daily[14]).isEqualTo(Pence(1500))
        assertThat(daily[1]).isEqualTo(Pence.ZERO)
    }

    @Test fun `weekdayTotals buckets Monday-first`() {
        // 2026-06-15 is a Monday, 2026-06-17 a Wednesday.
        val weekday = weekdayTotals(listOf(tx(1000, "2026-06-15T09:00:00Z"), tx(500, "2026-06-17T09:00:00Z")), utc)
        assertThat(weekday).hasSize(7)
        assertThat(weekday[0]).isEqualTo(Pence(1000))
        assertThat(weekday[2]).isEqualTo(Pence(500))
        assertThat(weekday[1]).isEqualTo(Pence.ZERO)
    }

    @Test fun `forecast projects from the pace so far`() {
        assertThat(forecastMonthEnd(Pence(1000), dayOfMonth = 10, daysInMonth = 30)).isEqualTo(Pence(3000))
        assertThat(forecastMonthEnd(Pence(500), dayOfMonth = 0, daysInMonth = 30)).isEqualTo(Pence(500))
    }
}
