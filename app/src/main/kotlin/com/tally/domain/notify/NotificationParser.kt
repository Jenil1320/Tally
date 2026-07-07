package com.tally.domain.notify

import com.tally.core.money.Pence
import kotlinx.datetime.Instant

/**
 * The human-readable content pulled off a single posted notification. Pure data,
 * no Android types — the [NotificationListenerService][android.service.notification.NotificationListenerService]
 * reads the `Notification.extras` bundle and hands one of these to [parsePayment].
 *
 * @property title `EXTRA_TITLE`. For Google Wallet (UK) this is the **merchant**
 *   (e.g. "SAINSBURYS", "TESCO STORES 6530"); for P2P it's often the brand name.
 * @property text `EXTRA_TEXT` — the collapsed one-liner. Google Wallet puts the
 *   amount + card here, e.g. "£1.70 with Yonder ••0767".
 * @property bigText `EXTRA_BIG_TEXT` (the expanded line, when present).
 * @property postedAtMillis `StatusBarNotification.postTime`.
 */
data class RawNotification(
    val packageName: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val postedAtMillis: Long,
)

/** A completed **outgoing** payment extracted from a [RawNotification]. */
data class ParsedPayment(
    val amount: Pence,
    val merchant: String,
    val occurredAt: Instant,
)

/** Fallback merchant when a debit is clearly a spend but no name could be read. */
private const val UNKNOWN_MERCHANT = "Card payment"

/** Brand/app words stripped from a title before using it as a merchant. */
private val BRAND_WORDS =
    listOf("google pay", "google wallet", "gpay", "google")

/** Words that mark a spend leaving the account. */
private val DEBIT_VERBS =
    listOf("you paid", "paid to", "paid ", " paid", "spent", "you sent", "sent ", "debited", "charged", "purchase")

/**
 * Signals the notification is money **in** (not a spend) — parsing bails out.
 * A GBP expense app must never book a credit or refund as spending.
 */
private val CREDIT_SIGNALS =
    listOf("received", "credited", "refund", "money added", "added to your", "you got", "cashback")

/**
 * Signals the notification is promotional or a money *request*, not a completed
 * payment — parsing bails out. Google Pay/Wallet pushes these from the same app.
 */
private val NOISE_SIGNALS =
    listOf(
        "requesting", "requested", "request money", "reward", "scratch", "offer",
        "voucher", "you won", "you earned", "earn ",
    )

/** £ amount: optional space, thousands-grouped whole part, optional 1–2 decimal digits. */
private val AMOUNT_REGEX = Regex("""£\s?([0-9][0-9,]*)(?:\.([0-9]{1,2}))?""")

/** "…to <merchant>" (P2P / bill) — capture up to a trailing clause or end of line. */
private val TO_MERCHANT_REGEX =
    Regex(
        """(?:paid|sent)\s+(?:£[0-9.,]+\s+)?to\s+(.+?)(?:\s+is\s+successful|\s+on\s|\s+using\s|\s+via\s|[.\n]|$)""",
        RegexOption.IGNORE_CASE,
    )

/** "…at <merchant>" (card purchase, e.g. "You spent £4.20 at Pret A Manger"). */
private val AT_MERCHANT_REGEX =
    Regex("""\bat\s+(.+?)(?:\s+on\s|\s+using\s|\s+via\s|[.\n]|$)""", RegexOption.IGNORE_CASE)

/** Google Wallet's card line, e.g. "£1.70 with Yonder ••0767" — a strong purchase signal. */
private val WITH_CARD_REGEX =
    Regex("""£\s?[0-9][0-9,]*(?:\.[0-9]{1,2})?\s+with\s+\S""", RegexOption.IGNORE_CASE)

/** A masked card number, e.g. "••0767" — glyph-tolerant. */
private val CARD_MASK_REGEX = Regex("""[•·∙・‧⋅*]+\s?\d{3,4}""")

/**
 * Extracts a completed outgoing payment from a payment-app notification, or null
 * when the text isn't a clear GBP spend. Deterministic and pure (no AI, no
 * Android) — the app is local-first and this is unit-tested without a device.
 *
 * Handles the two real shapes seen from Google Pay / Wallet:
 *  - **Card purchase (UK Wallet):** merchant in the *title* ("SAINSBURYS"), body
 *    "£1.70 with Yonder ••0767" — no verb, so the `£… with …`/card-mask is the
 *    purchase signal.
 *  - **P2P / older:** "You paid £12.00 to Tesco", "You spent £4.20 at Pret" —
 *    a debit verb and an inline "to/at" merchant.
 *
 * A notification qualifies only when: no credit/refund signal and no
 * promo/request signal; a positive `£` amount is present; a merchant can be
 * resolved (inline "to/at", else the title); and there is a purchase signal
 * (debit verb, inline merchant, or the Wallet card line). The amount is integer
 * pence (never `Float`; £4.20 → 420) and [occurredAt] is the post time.
 */
fun parsePayment(raw: RawNotification): ParsedPayment? {
    val blob = listOfNotNull(raw.title, raw.text, raw.bigText).joinToString("\n").trim()
    if (blob.isBlank()) return null

    val lower = blob.lowercase()
    if (CREDIT_SIGNALS.any { lower.contains(it) }) return null
    if (NOISE_SIGNALS.any { lower.contains(it) }) return null

    val amount = extractAmount(blob) ?: return null
    if (amount.value <= 0) return null

    val inlineMerchant = extractMerchant(blob)
    val hasDebitVerb = DEBIT_VERBS.any { lower.contains(it) }
    val hasCardLine = WITH_CARD_REGEX.containsMatchIn(blob) || CARD_MASK_REGEX.containsMatchIn(blob)

    val purchaseSignal = hasDebitVerb || inlineMerchant != null || hasCardLine
    if (!purchaseSignal) return null

    val merchant =
        inlineMerchant
            ?: titleMerchant(raw.title)
            ?: if (hasDebitVerb || hasCardLine) UNKNOWN_MERCHANT else return null

    return ParsedPayment(
        amount = amount,
        merchant = merchant,
        occurredAt = Instant.fromEpochMilliseconds(raw.postedAtMillis),
    )
}

private fun extractAmount(blob: String): Pence? {
    val match = AMOUNT_REGEX.find(blob) ?: return null
    val whole = match.groupValues[1].replace(",", "").toLongOrNull() ?: return null
    val fraction = match.groupValues[2]
    val pennies =
        when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 10 // "£5.5" -> 50p
            else -> fraction.toLong() // already two digits
        }
    return Pence(whole * 100 + pennies)
}

private fun extractMerchant(blob: String): String? {
    for (line in blob.lines()) {
        (TO_MERCHANT_REGEX.find(line) ?: AT_MERCHANT_REGEX.find(line))?.let { match ->
            cleanMerchant(match.groupValues[1])?.let { return it }
        }
    }
    return null
}

/** Uses the title as a merchant only after stripping brand words (e.g. "Google Pay"). */
private fun titleMerchant(title: String?): String? {
    val base = title?.takeIf { it.isNotBlank() } ?: return null
    val stripped = BRAND_WORDS.fold(base) { acc, brand -> acc.replace(brand, "", ignoreCase = true) }
    return cleanMerchant(stripped)
}

private fun cleanMerchant(raw: String): String? {
    val cleaned = raw.replace(Regex("""\s+"""), " ").trim().trim('-', '·', ':', ',').trim()
    return cleaned.ifBlank { null }?.take(60)
}
