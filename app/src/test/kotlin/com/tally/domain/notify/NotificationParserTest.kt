package com.tally.domain.notify

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class NotificationParserTest {
    private val gpay = "com.google.android.apps.walletnfcrel"

    private fun notif(
        title: String? = "Google Pay",
        text: String? = null,
        bigText: String? = null,
        postedAtMillis: Long = 1_700_000_000_000L,
    ) = RawNotification(gpay, title, text, bigText, postedAtMillis)

    @Test fun `real Google Wallet UK purchase — merchant in title, card line in body`() {
        // Verbatim from the owner's Pixel 7 (2026-07-07).
        val sainsburys = parsePayment(notif(title = "SAINSBURYS", text = "£1.70 with Yonder ••0767"))
        assertThat(sainsburys).isNotNull()
        assertThat(sainsburys!!.amount.value).isEqualTo(170L)
        assertThat(sainsburys.merchant).isEqualTo("SAINSBURYS")

        val tesco = parsePayment(notif(title = "TESCO STORES 6530", text = "£12.50 with Yonder ••0767"))
        assertThat(tesco!!.amount.value).isEqualTo(1250L)
        assertThat(tesco.merchant).isEqualTo("TESCO STORES 6530")
    }

    @Test fun `Wallet card line is a purchase signal even without a bullet mask`() {
        val result = parsePayment(notif(title = "COSTA COFFEE", text = "£3.30 with Visa"))
        assertThat(result!!.amount.value).isEqualTo(330L)
        assertThat(result.merchant).isEqualTo("COSTA COFFEE")
    }

    @Test fun `cashback in the card name is not mistaken for a credit`() {
        // Verbatim from the owner's Pixel 7 — the card is a "Platinum Cashback" card.
        val result = parsePayment(notif(title = "AMAZON CO UK RETAIL", text = "£36.99 with Platinum Cashback Everyday Credit Card ••1001"))
        assertThat(result).isNotNull()
        assertThat(result!!.amount.value).isEqualTo(3699L)
        assertThat(result.merchant).isEqualTo("AMAZON CO UK RETAIL")
    }

    @Test fun `a genuine cashback credit is still ignored`() {
        assertThat(parsePayment(notif(title = "Google Pay", text = "You earned £5.00 cashback"))).isNull()
    }

    @Test fun `card purchase with 'at' merchant`() {
        val result = parsePayment(notif(title = "Google Wallet", text = "You spent £4.20 at Pret A Manger"))
        assertThat(result).isNotNull()
        assertThat(result!!.amount.value).isEqualTo(420L)
        assertThat(result.merchant).isEqualTo("Pret A Manger")
    }

    @Test fun `p2p payment with 'to' merchant`() {
        val result = parsePayment(notif(text = "You paid £12.00 to Tesco"))
        assertThat(result!!.amount.value).isEqualTo(1200L)
        assertThat(result.merchant).isEqualTo("Tesco")
    }

    @Test fun `'is successful' clause is trimmed from the merchant`() {
        val result = parsePayment(notif(text = "£5 paid to Tesco is successful"))
        assertThat(result!!.amount.value).isEqualTo(500L)
        assertThat(result.merchant).isEqualTo("Tesco")
    }

    @Test fun `card suffix is trimmed from the merchant`() {
        val result = parsePayment(notif(text = "You spent £4.20 at Pret A Manger using Visa"))
        assertThat(result!!.merchant).isEqualTo("Pret A Manger")
    }

    @Test fun `thousands grouping parses to pence`() {
        val result = parsePayment(notif(text = "You paid £1,234.56 to Landlord"))
        assertThat(result!!.amount.value).isEqualTo(123456L)
        assertThat(result.merchant).isEqualTo("Landlord")
    }

    @Test fun `single decimal digit is tens of pence`() {
        val result = parsePayment(notif(text = "You paid £5.5 to Sam"))
        assertThat(result!!.amount.value).isEqualTo(550L)
    }

    @Test fun `whole pounds with no decimals`() {
        val result = parsePayment(notif(text = "You spent £15 at Boots"))
        assertThat(result!!.amount.value).isEqualTo(1500L)
        assertThat(result.merchant).isEqualTo("Boots")
    }

    @Test fun `amount read from bigText`() {
        val result = parsePayment(notif(text = null, bigText = "You paid £3.30 to Costa Coffee"))
        assertThat(result!!.amount.value).isEqualTo(330L)
        assertThat(result.merchant).isEqualTo("Costa Coffee")
    }

    @Test fun `debit with no merchant falls back to a generic label`() {
        val result = parsePayment(notif(title = "Google Pay", text = "You paid £8.00"))
        assertThat(result!!.amount.value).isEqualTo(800L)
        assertThat(result.merchant).isEqualTo("Card payment")
    }

    @Test fun `occurredAt is the post time`() {
        val result = parsePayment(notif(text = "You paid £1.00 to Sam", postedAtMillis = 1_699_999_999_000L))
        assertThat(result!!.occurredAt.toEpochMilliseconds()).isEqualTo(1_699_999_999_000L)
    }

    @Test fun `money received is not a spend`() {
        assertThat(parsePayment(notif(text = "Received £50 from John"))).isNull()
    }

    @Test fun `refund is not a spend`() {
        assertThat(parsePayment(notif(text = "You were refunded £9.99 by ASOS"))).isNull()
    }

    @Test fun `money request is not a spend`() {
        assertThat(parsePayment(notif(text = "Sarah is requesting £10"))).isNull()
    }

    @Test fun `promotional reward is not a spend`() {
        assertThat(parsePayment(notif(text = "You have a new £5 reward waiting"))).isNull()
    }

    @Test fun `notification with no amount is ignored`() {
        assertThat(parsePayment(notif(text = "Payment successful"))).isNull()
    }

    @Test fun `bare amount with no verb or merchant is too ambiguous`() {
        assertThat(parsePayment(notif(title = "Google Pay", text = "£8.00"))).isNull()
    }

    @Test fun `blank notification is ignored`() {
        assertThat(parsePayment(notif(title = null, text = null, bigText = null))).isNull()
    }
}
