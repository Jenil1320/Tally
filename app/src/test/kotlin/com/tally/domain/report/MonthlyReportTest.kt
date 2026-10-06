package com.tally.domain.report

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.domain.model.Category
import com.tally.domain.model.Transaction
import com.tally.domain.model.TransactionSource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.jupiter.api.Test

class MonthlyReportTest {
    private val tz = TimeZone.UTC
    private val july = LocalDate(2026, 7, 1)
    private val categories =
        listOf(
            Category("g", "Groceries", 0, true, 0),
            Category("e", "Eating out", 1, true, 1),
        )

    private var counter = 0

    private fun tx(
        amount: Long,
        merchant: String,
        day: Int,
        categoryId: String?,
        source: TransactionSource = TransactionSource.MANUAL,
    ): Transaction {
        val at = LocalDate(2026, 7, day).atStartOfDayIn(tz)
        return Transaction("t${counter++}", Pence(amount), merchant, null, categoryId, at, at, source)
    }

    private val sample =
        listOf(
            tx(4000, "Tesco", 5, "g", TransactionSource.MANUAL),
            tx(2000, "Tesco", 10, "g", TransactionSource.NOTIFICATION),
            tx(1000, "TESCO", 12, "g", TransactionSource.NOTIFICATION),
            tx(1500, "Deliveroo", 8, "e", TransactionSource.NOTIFICATION),
        )

    @Test fun `totals, count and label`() {
        val r = monthlyReport(sample, categories, previousMonthTotal = null, firstOfMonth = july, timeZone = tz)
        assertThat(r.monthLabel).isEqualTo("July 2026")
        assertThat(r.total.value).isEqualTo(8500L)
        assertThat(r.transactionCount).isEqualTo(4)
        assertThat(r.isEmpty).isFalse()
    }

    @Test fun `category breakdown is largest first`() {
        val r = monthlyReport(sample, categories, null, july, tz)
        assertThat(r.byCategory.map { it.name }).containsExactly("Groceries", "Eating out").inOrder()
        assertThat(r.byCategory.first().total.value).isEqualTo(7000L)
    }

    @Test fun `top merchant merges case-insensitively`() {
        val r = monthlyReport(sample, categories, null, july, tz)
        assertThat(r.topMerchant?.merchant).isEqualTo("Tesco")
        assertThat(r.topMerchant?.total?.value).isEqualTo(7000L)
        assertThat(r.topMerchant?.visits).isEqualTo(3)
    }

    @Test fun `priciest day is the biggest single-day total`() {
        val r = monthlyReport(sample, categories, null, july, tz)
        assertThat(r.priciestDay?.date).isEqualTo(LocalDate(2026, 7, 5))
        assertThat(r.priciestDay?.total?.value).isEqualTo(4000L)
    }

    @Test fun `avg per day divides by month length and auto percent counts notifications`() {
        val r = monthlyReport(sample, categories, null, july, tz)
        assertThat(r.avgPerDay.value).isEqualTo(8500L / 31)
        assertThat(r.autoCapturedPercent).isEqualTo(75) // 3 of 4
    }

    @Test fun `month-over-month delta is signed against the previous total`() {
        val r = monthlyReport(sample, categories, previousMonthTotal = Pence(10000), firstOfMonth = july, timeZone = tz)
        assertThat(r.vsPrevious?.previousTotal?.value).isEqualTo(10000L)
        assertThat(r.vsPrevious?.deltaPercent).isEqualTo(-15)
    }

    @Test fun `previous total of zero or null omits the delta`() {
        assertThat(monthlyReport(sample, categories, null, july, tz).vsPrevious).isNull()
        assertThat(monthlyReport(sample, categories, Pence(0), july, tz).vsPrevious).isNull()
    }

    @Test fun `eating out reports the share, and the report totals stay at the full amount`() {
        val shared =
            tx(6000, "Dishoom", 14, "e").copy(myShare = Pence(1500))
        val r = monthlyReport(sample + shared, categories, null, july, tz)
        assertThat(r.total.value).isEqualTo(14_500L)
        assertThat(r.eatingOut!!.paid.value).isEqualTo(7_500L) // 1,500 + 6,000
        assertThat(r.eatingOut!!.mine.value).isEqualTo(3_000L) // 1,500 + 1,500 share
        assertThat(r.eatingOut!!.hasShared).isTrue()
    }

    @Test fun `no eating out spend means no eating out summary`() {
        val r = monthlyReport(listOf(tx(500, "Tesco", 3, "g")), categories, null, july, tz)
        assertThat(r.eatingOut).isNull()
    }

    @Test fun `an empty month is empty`() {
        val r = monthlyReport(emptyList(), categories, null, july, tz)
        assertThat(r.isEmpty).isTrue()
        assertThat(r.total.value).isEqualTo(0L)
        assertThat(r.topMerchant).isNull()
        assertThat(r.priciestDay).isNull()
        assertThat(r.autoCapturedPercent).isEqualTo(0)
    }
}
