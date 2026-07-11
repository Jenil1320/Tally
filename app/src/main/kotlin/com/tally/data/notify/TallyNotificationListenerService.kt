package com.tally.data.notify

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.tally.core.money.MoneyFormat
import com.tally.di.IoDispatcher
import com.tally.domain.category.MerchantRuleStore
import com.tally.domain.category.suggestCategory
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.TransactionSource
import com.tally.domain.notify.NotificationIngestPreferences
import com.tally.domain.notify.RawNotification
import com.tally.domain.notify.categoryCandidates
import com.tally.domain.notify.parsePayment
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import com.tally.work.CaptureNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reads notifications posted by the allow-listed payment apps (Google Pay /
 * Wallet) and auto-logs clear GBP debits as [TransactionSource.NOTIFICATION]
 * transactions.
 *
 * Bound by the system once the user grants "Notification access" in Settings;
 * gated additionally by [NotificationIngestPreferences] so the user can pause
 * capture in-app. Parsing is delegated to the pure [parsePayment]; DB writes
 * happen off the main thread.
 *
 * Captures both live posts ([onNotificationPosted]) **and** whatever is already
 * in the shade when the listener (re)connects ([onListenerConnected] scans
 * [getActiveNotifications]) — so a payment that arrived before the service was
 * bound/armed still gets picked up. De-duplication is durable: a payment already
 * stored (same amount/merchant/time) is skipped, so rescans never double-log.
 * Raw notification text is not persisted (see PRIVACY.md / ADR-0009).
 */
@AndroidEntryPoint
class TallyNotificationListenerService : NotificationListenerService() {
    @Inject lateinit var transactionRepository: TransactionRepository

    @Inject lateinit var categoryRepository: CategoryRepository

    @Inject lateinit var merchantRuleStore: MerchantRuleStore

    @Inject lateinit var ingestPreferences: NotificationIngestPreferences

    @Inject @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private lateinit var scope: CoroutineScope

    /** Bounded set of recently-handled notification keys, to drop rapid re-posts cheaply. */
    private val recentKeys =
        object : LinkedHashMap<String, Boolean>(RECENT_CAPACITY, LOAD_FACTOR, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>) = size > RECENT_CAPACITY
        }

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val active =
            try {
                activeNotifications
            } catch (e: SecurityException) {
                Log.w(TAG, "getActiveNotifications failed", e)
                null
            } ?: return
        Log.i(TAG, "connected; scanning ${active.size} active notifications")
        active.forEach { handle(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = handle(sbn)

    private fun handle(sbn: StatusBarNotification) {
        if (sbn.packageName !in PAYMENT_PACKAGES) return
        val notification = sbn.notification ?: return
        // Skip the group-summary parent — its children carry the real amount/merchant.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = notification.extras ?: return
        val raw =
            RawNotification(
                packageName = sbn.packageName,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                postedAtMillis = sbn.postTime,
            )
        if (recentKeys.put(sbn.key + "|" + raw.text, true) != null) return

        scope.launch {
            if (!ingestPreferences.enabled.first()) {
                Log.i(TAG, "capture off; ignoring ${sbn.packageName}")
                return@launch
            }
            val parsed = parsePayment(raw)
            if (parsed == null) {
                Log.i(TAG, "not a payment from ${sbn.packageName}: title='${raw.title}'")
                return@launch
            }
            if (transactionRepository.notificationTransactionExists(parsed.amount, parsed.merchant, parsed.occurredAt)) {
                Log.i(TAG, "duplicate; skipping ${parsed.merchant} ${MoneyFormat.format(parsed.amount)}")
                return@launch
            }
            val categories = categoryRepository.observeAll().first()
            val learned = merchantRuleStore.rules.first()
            val categoryId = suggestCategory(parsed.merchant, categories, learned)
            val newId =
                transactionRepository.add(
                    NewTransaction(
                        amount = parsed.amount,
                        merchant = parsed.merchant,
                        occurredAt = parsed.occurredAt,
                        categoryId = categoryId,
                        source = TransactionSource.NOTIFICATION,
                    ),
                )
            Log.i(TAG, "logged ${parsed.merchant} ${MoneyFormat.format(parsed.amount)} id=$newId")
            val chosen = categories.find { it.id == categoryId }
            val candidates =
                if (chosen == null) {
                    categoryCandidates(transactionRepository.topCategoryIds(CANDIDATE_COUNT), categories, CANDIDATE_COUNT)
                } else {
                    emptyList()
                }
            CaptureNotifier.show(
                context = applicationContext,
                transactionId = newId,
                merchant = parsed.merchant,
                amountText = MoneyFormat.format(parsed.amount),
                categoryName = chosen?.name,
                candidates = candidates,
            )
        }
    }

    override fun onDestroy() {
        if (::scope.isInitialized) scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TallyCapture"
        private const val RECENT_CAPACITY = 200
        private const val LOAD_FACTOR = 0.75f
        private const val CANDIDATE_COUNT = 3

        /**
         * Packages whose notifications we read. Google Wallet is the UK/global
         * app; the India UPI package and Play services are included because UK
         * tap-to-pay purchase notifications have historically been posted by
         * `com.google.android.gms` rather than Wallet (see ADR-0009). The strict
         * [parsePayment] gating keeps non-payment Play-services noise out.
         */
        val PAYMENT_PACKAGES =
            setOf(
                "com.google.android.apps.walletnfcrel",
                "com.google.android.apps.nbu.paisa.user",
                "com.google.android.gms",
            )
    }
}
