package com.tally.domain.insight

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.domain.model.Category
import com.tally.domain.model.Transaction
import com.tally.domain.model.TransactionSource
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

class PatternsTest {
    private val utc = TimeZone.UTC
    private val eating = Category("c-eat", "Eating out", 1, true, 1)
    private val gifts = Category("c-gift", "Gifts", 2, true, 2)
    private val categories = listOf(eating, gifts)
    private var counter = 0

    private fun tx(
        date: String,
        amount: Long,
        categoryId: String? = null,
        myShare: Long? = null,
        merchant: String = "Shop",
    ): Transaction {
        val at = Instant.parse("${date}T12:00:00Z")
        return Transaction(
            id = "t${counter++}",
            amount = Pence(amount),
            merchant = merchant,
            note = null,
            categoryId = categoryId,
            occurredAt = at,
            createdAt = at,
            source = TransactionSource.MANUAL,
            myShare = myShare?.let(::Pence),
        )
    }

    // ---- six-month trend -------------------------------------------------

    @Test fun `monthly totals run oldest first and end at the current month`() {
        val result =
            monthlyTotals(
                listOf(tx("2026-10-02", 500), tx("2026-10-05", 250), tx("2026-08-20", 1_000), tx("2026-04-01", 9_999)),
                utc,
                LocalDate(2026, 10, 15),
            )
        assertThat(result.map { it.shortLabel }).containsExactly("May", "Jun", "Jul", "Aug", "Sep", "Oct").inOrder()
        assertThat(result.map { it.total.value }).containsExactly(0L, 0L, 0L, 1_000L, 0L, 750L).inOrder()
        assertThat(result.last().fullLabel).isEqualTo("October 2026")
    }

    @Test fun `monthly totals cross a year boundary`() {
        val result = monthlyTotals(listOf(tx("2025-12-31", 300), tx("2026-01-01", 700)), utc, LocalDate(2026, 2, 10))
        assertThat(result.map { it.shortLabel }).containsExactly("Sep", "Oct", "Nov", "Dec", "Jan", "Feb").inOrder()
        assertThat(result[3].total.value).isEqualTo(300L)
        assertThat(result[4].total.value).isEqualTo(700L)
        assertThat(result[3].fullLabel).isEqualTo("December 2025")
    }

    // ---- a typical week --------------------------------------------------

    @Test fun `weekday averages divide by how many of that weekday the window holds`() {
        // 2026-10-05 is a Monday. The window 5 Oct to 18 Oct holds two Mondays.
        val result =
            weekdayAverages(
                listOf(tx("2026-10-05", 1_000), tx("2026-10-12", 3_000), tx("2026-10-10", 600)),
                utc,
                from = LocalDate(2026, 10, 5),
                to = LocalDate(2026, 10, 18),
            )
        assertThat(result[0]).isEqualTo(Pence(2_000)) // Monday: 4,000 over 2 Mondays
        assertThat(result[5]).isEqualTo(Pence(300)) // Saturday: 600 over 2 Saturdays
        assertThat(result[2]).isEqualTo(Pence.ZERO)
    }

    @Test fun `weekday averages ignore spend outside the window`() {
        val result =
            weekdayAverages(listOf(tx("2026-09-28", 5_000)), utc, LocalDate(2026, 10, 5), LocalDate(2026, 10, 11))
        assertThat(result.all { it == Pence.ZERO }).isTrue()
    }

    @Test fun `the week rhythm splits weekdays from the weekend`() {
        val rhythm = weekRhythm(listOf(1_000L, 1_000L, 1_000L, 1_000L, 1_000L, 4_000L, 2_000L).map(::Pence))!!
        assertThat(rhythm.weekdayAverage).isEqualTo(Pence(1_000))
        assertThat(rhythm.weekendAverage).isEqualTo(Pence(3_000))
        assertThat(rhythm.weekendHeavier).isTrue()
        assertThat(weekRhythm(emptyList())).isNull()
    }

    // ---- unusual spends --------------------------------------------------

    private fun giftHistory(): List<Transaction> =
        listOf(
            tx("2026-06-03", 1_000, "c-gift"),
            tx("2026-07-03", 1_200, "c-gift"),
            tx("2026-08-03", 900, "c-gift"),
            tx("2026-09-03", 1_100, "c-gift"),
            tx("2026-09-20", 1_000, "c-gift"),
        )

    @Test fun `a purchase far above the category's usual is flagged with how many times usual`() {
        val big = tx("2026-10-04", 5_000, "c-gift", merchant = "Fabric")
        val result = unusualSpends(giftHistory() + big, categories, since = Instant.parse("2026-10-01T00:00:00Z"))
        assertThat(result).hasSize(1)
        assertThat(result.single().merchant).isEqualTo("Fabric")
        assertThat(result.single().usual).isEqualTo(Pence(1_040)) // mean of the five others
        assertThat(result.single().times).isWithin(0.05f).of(5_000f / 1_040f)
        assertThat(result.single().categoryName).isEqualTo("Gifts")
    }

    @Test fun `an ordinary purchase is not flagged`() {
        val ordinary = tx("2026-10-04", 1_250, "c-gift")
        assertThat(unusualSpends(giftHistory() + ordinary, categories, Instant.parse("2026-10-01T00:00:00Z"))).isEmpty()
    }

    @Test fun `a category with too little history flags nothing`() {
        val thin = listOf(tx("2026-09-03", 1_000, "c-gift"), tx("2026-09-10", 1_100, "c-gift"), tx("2026-10-04", 9_000, "c-gift"))
        assertThat(unusualSpends(thin, categories, Instant.parse("2026-10-01T00:00:00Z"))).isEmpty()
    }

    @Test fun `only purchases since the cutoff are reported, and uncategorised spend is skipped`() {
        val oldBig = tx("2026-08-15", 5_000, "c-gift")
        val loose = tx("2026-10-04", 9_000)
        val result = unusualSpends(giftHistory() + oldBig + loose, categories, Instant.parse("2026-10-01T00:00:00Z"))
        assertThat(result).isEmpty()
    }

    @Test fun `the biggest ratio comes first and the list is capped`() {
        val history = giftHistory()
        val a = tx("2026-10-02", 3_000, "c-gift")
        val b = tx("2026-10-03", 6_000, "c-gift")
        val result = unusualSpends(history + a + b, categories, Instant.parse("2026-10-01T00:00:00Z"), limit = 1)
        assertThat(result.map { it.amount.value }).containsExactly(6_000L)
    }

    // ---- eating out by month ---------------------------------------------

    @Test fun `eating out by month lists months with eating out, newest first, with the share`() {
        val result =
            eatingOutByMonth(
                listOf(
                    tx("2026-10-02", 6_000, "c-eat", myShare = 1_500),
                    tx("2026-10-09", 2_000, "c-eat"),
                    tx("2026-08-20", 4_000, "c-eat"),
                    tx("2026-10-03", 9_999, "c-gift"),
                ),
                categories,
                utc,
                LocalDate(2026, 10, 15),
            )
        assertThat(result.map { it.label }).containsExactly("October 2026", "August 2026").inOrder()
        assertThat(result[0].paid).isEqualTo(Pence(8_000))
        assertThat(result[0].mine).isEqualTo(Pence(3_500))
        assertThat(result[0].hasShared).isTrue()
        assertThat(result[1].hasShared).isFalse()
        assertThat(result[1].mine).isEqualTo(result[1].paid)
    }

    @Test fun `no eating out means an empty list`() {
        assertThat(eatingOutByMonth(listOf(tx("2026-10-02", 500, "c-gift")), categories, utc, LocalDate(2026, 10, 15))).isEmpty()
    }
}
