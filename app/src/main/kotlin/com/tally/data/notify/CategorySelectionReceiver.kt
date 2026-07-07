package com.tally.data.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tally.di.IoDispatcher
import com.tally.domain.category.MerchantRuleStore
import com.tally.domain.category.normaliseMerchant
import com.tally.domain.repo.TransactionRepository
import com.tally.work.CaptureNotifier
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handles a tap on one of the category buttons on a capture notification. Sets
 * the transaction's category, teaches the merchant→category rule (so the same
 * merchant auto-categorises next time — the same learning the Add sheet does on
 * save), and updates the notification to confirm the choice. Runs without
 * opening the app; [goAsync] keeps the process alive for the short DB write.
 *
 * Dependencies are pulled through a Hilt [EntryPoint] rather than
 * `@AndroidEntryPoint` field injection, so there is no `super.onReceive` call
 * (`BroadcastReceiver.onReceive` is abstract).
 */
class CategorySelectionReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun transactionRepository(): TransactionRepository

        fun merchantRuleStore(): MerchantRuleStore

        @IoDispatcher
        fun ioDispatcher(): CoroutineDispatcher
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val transactionId = intent.getStringExtra(EXTRA_TX_ID) ?: return
        val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID) ?: return
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME)
        val merchant = intent.getStringExtra(EXTRA_MERCHANT).orEmpty()
        val amountText = intent.getStringExtra(EXTRA_AMOUNT_TEXT).orEmpty()
        val appContext = context.applicationContext
        val deps = EntryPointAccessors.fromApplication(appContext, Deps::class.java)

        val pending = goAsync()
        CoroutineScope(deps.ioDispatcher()).launch {
            try {
                deps.transactionRepository().updateCategory(transactionId, categoryId)
                if (merchant.isNotBlank()) {
                    deps.merchantRuleStore().learn(normaliseMerchant(merchant), categoryId)
                }
                CaptureNotifier.show(
                    context = appContext,
                    transactionId = transactionId,
                    merchant = merchant,
                    amountText = amountText,
                    categoryName = categoryName,
                    candidates = emptyList(),
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val EXTRA_TX_ID = "com.tally.extra.TX_ID"
        private const val EXTRA_CATEGORY_ID = "com.tally.extra.CATEGORY_ID"
        private const val EXTRA_CATEGORY_NAME = "com.tally.extra.CATEGORY_NAME"
        private const val EXTRA_MERCHANT = "com.tally.extra.MERCHANT"
        private const val EXTRA_AMOUNT_TEXT = "com.tally.extra.AMOUNT_TEXT"

        /** Builds the explicit broadcast intent a category button fires. */
        fun selectionIntent(
            context: Context,
            transactionId: String,
            categoryId: String,
            categoryName: String,
            merchant: String,
            amountText: String,
        ): Intent =
            Intent(context, CategorySelectionReceiver::class.java).apply {
                putExtra(EXTRA_TX_ID, transactionId)
                putExtra(EXTRA_CATEGORY_ID, categoryId)
                putExtra(EXTRA_CATEGORY_NAME, categoryName)
                putExtra(EXTRA_MERCHANT, merchant)
                putExtra(EXTRA_AMOUNT_TEXT, amountText)
            }
    }
}
