package com.tally.domain.insight

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.domain.model.Transaction
import com.tally.domain.model.TransactionSource
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class MerchantTotalsTest {
    private var counter = 0

    private fun tx(
        amount: Long,
        merchant: String,
    ) = Transaction(
        id = "t${counter++}",
        amount = Pence(amount),
        merchant = merchant,
        note = null,
        categoryId = null,
        occurredAt = Instant.fromEpochMilliseconds(0),
        createdAt = Instant.fromEpochMilliseconds(0),
        source = TransactionSource.NOTIFICATION,
    )

    @Test fun `store numbers and suffixes group to one merchant`() {
        assertThat(merchantKey("TESCO STORES 6530")).isEqualTo(merchantKey("Tesco"))
        assertThat(merchantKey("SAINSBURYS")).isEqualTo(merchantKey("sainsburys"))
        assertThat(merchantKey("Deliveroo Ltd")).isEqualTo(merchantKey("DELIVEROO"))
    }

    @Test fun `distinct merchants stay distinct`() {
        assertThat(merchantKey("TESCO")).isNotEqualTo(merchantKey("SAINSBURYS"))
        assertThat(merchantKey("AMAZON CO UK RETAIL")).isNotEqualTo(merchantKey("TESCO"))
    }

    @Test fun `totals group by merchant, largest first`() {
        val result =
            merchantTotals(
                listOf(
                    tx(1000, "TESCO STORES 6530"),
                    tx(2000, "Tesco"),
                    tx(2500, "AMAZON CO UK RETAIL"),
                ),
            )
        assertThat(result.map { it.total.value }).containsExactly(3000L, 2500L).inOrder()
        assertThat(result.first().visits).isEqualTo(2)
    }

    @Test fun `small and often outweighs a single large purchase`() {
        val result =
            merchantTotals(
                listOf(
                    tx(4000, "Amazon"),
                    tx(900, "Pret"),
                    tx(850, "Pret"),
                    tx(1100, "Pret"),
                    tx(1300, "Pret"),
                ),
            )
        assertThat(result.first().merchant).isEqualTo("Pret")
        assertThat(result.first().total.value).isEqualTo(4150L)
        assertThat(result.first().visits).isEqualTo(4)
    }

    @Test fun `display name is the most common spelling`() {
        val result = merchantTotals(listOf(tx(100, "Tesco"), tx(100, "Tesco"), tx(100, "TESCO STORES 6530")))
        assertThat(result.single().merchant).isEqualTo("Tesco")
    }

    @Test fun `the limit caps the result and empties stay empty`() {
        val many = (1..10).map { tx(it * 100L, "Shop $it") }
        assertThat(merchantTotals(many, limit = 3)).hasSize(3)
        assertThat(merchantTotals(emptyList())).isEmpty()
        assertThat(merchantTotals(many, limit = 0)).isEmpty()
    }
}
