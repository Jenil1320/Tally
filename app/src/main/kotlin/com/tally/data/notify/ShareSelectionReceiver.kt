package com.tally.data.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.tally.core.money.MoneyFormat
import com.tally.di.IoDispatcher
import com.tally.domain.notify.ShareReply
import com.tally.domain.notify.parseShareReply
import com.tally.domain.repo.TransactionRepository
import com.tally.work.CaptureNotifier
import com.tally.work.CaptureShare
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handles the share actions on an Eating out capture notification: "Half", or an
 * amount typed into the "My share" reply box. It saves the owner's share next to
 * the full amount paid (ADR-0010) and posts the notification again as a
 * confirmation that shows the share. Nothing opens; [goAsync] keeps the process
 * alive for the short database write.
 *
 * An amount that cannot be read, or that is above what was paid, changes
 * nothing. The notification is posted again with a short hint and the same
 * actions, so the reply box never hangs on a spinner.
 *
 * Dependencies come through a Hilt [EntryPoint], as in [CategorySelectionReceiver].
 */
class ShareSelectionReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun transactionRepository(): TransactionRepository

        @IoDispatcher
        fun ioDispatcher(): CoroutineDispatcher
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val transactionId = intent.getStringExtra(EXTRA_TX_ID) ?: return
        val merchant = intent.getStringExtra(EXTRA_MERCHANT).orEmpty()
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME)
        val amountText = intent.getStringExtra(EXTRA_AMOUNT_TEXT).orEmpty()
        val typed = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(CaptureNotifier.KEY_SHARE_REPLY)
        val isHalf = intent.action == ACTION_HALF
        val appContext = context.applicationContext
        val deps = EntryPointAccessors.fromApplication(appContext, Deps::class.java)

        val pending = goAsync()
        CoroutineScope(deps.ioDispatcher()).launch {
            try {
                val repository = deps.transactionRepository()
                val transaction = repository.findById(transactionId) ?: return@launch
                val reply =
                    if (isHalf) {
                        ShareReply.Valid(transaction.amount.splitEvenly(2).first())
                    } else {
                        parseShareReply(typed, transaction.amount)
                    }
                val share =
                    when (reply) {
                        is ShareReply.Valid -> {
                            // A share equal to the amount paid is the same as "all mine": store nothing.
                            repository.updateShare(transactionId, reply.share.takeIf { it < transaction.amount })
                            CaptureShare.Saved(MoneyFormat.format(reply.share))
                        }
                        ShareReply.Unreadable -> CaptureShare.Retry("Could not read that. Try 12.50")
                        ShareReply.TooHigh -> CaptureShare.Retry("That is more than was paid")
                    }
                CaptureNotifier.show(
                    context = appContext,
                    transactionId = transactionId,
                    merchant = merchant,
                    amountText = amountText,
                    categoryName = categoryName,
                    candidates = emptyList(),
                    share = share,
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_HALF = "com.tally.action.SHARE_HALF"
        private const val ACTION_TYPED = "com.tally.action.SHARE_TYPED"
        private const val EXTRA_TX_ID = "com.tally.extra.TX_ID"
        private const val EXTRA_MERCHANT = "com.tally.extra.MERCHANT"
        private const val EXTRA_CATEGORY_NAME = "com.tally.extra.CATEGORY_NAME"
        private const val EXTRA_AMOUNT_TEXT = "com.tally.extra.AMOUNT_TEXT"

        /** The "Half" button: the share is half the amount paid (the odd penny stays with the owner). */
        fun halfIntent(
            context: Context,
            transactionId: String,
            merchant: String,
            categoryName: String,
            amountText: String,
        ): Intent = baseIntent(context, ACTION_HALF, transactionId, merchant, categoryName, amountText)

        /** The "My share" reply: the system adds the typed text before it is sent. */
        fun typedIntent(
            context: Context,
            transactionId: String,
            merchant: String,
            categoryName: String,
            amountText: String,
        ): Intent = baseIntent(context, ACTION_TYPED, transactionId, merchant, categoryName, amountText)

        private fun baseIntent(
            context: Context,
            action: String,
            transactionId: String,
            merchant: String,
            categoryName: String,
            amountText: String,
        ): Intent =
            Intent(context, ShareSelectionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_TX_ID, transactionId)
                putExtra(EXTRA_MERCHANT, merchant)
                putExtra(EXTRA_CATEGORY_NAME, categoryName)
                putExtra(EXTRA_AMOUNT_TEXT, amountText)
            }
    }
}
