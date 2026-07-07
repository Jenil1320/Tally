package com.tally.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tally.MainActivity
import com.tally.R
import com.tally.data.notify.CategorySelectionReceiver
import com.tally.domain.model.Category

/**
 * Posts the notification shown after an expense is auto-captured from a payment
 * notification (see ADR-0009). Two shapes from one call:
 *
 *  - **Categorised** — a quiet confirmation (`Logged £4.20 · Pret · Eating out`)
 *    with a single "Change" action that opens the entry for editing.
 *  - **Uncategorised** — the same line ending `· Pick a category`, plus up to
 *    three one-tap category buttons ([candidates]); tapping one is handled by
 *    [CategorySelectionReceiver] without opening the app. Tapping the body opens
 *    the full editor.
 *
 * Tone is observational per the constitution — no emoji, no exclamation marks.
 * The notification id is derived from the transaction id, so re-posting after a
 * category is chosen updates the same notification in place.
 */
object CaptureNotifier {
    const val CHANNEL_ID = "tally-capture"
    const val EXTRA_EDIT_TRANSACTION_ID = "com.tally.extra.EDIT_TRANSACTION_ID"

    private const val MAX_ACTIONS = 3

    /** Idempotently creates the capture notification channel. Safe on every launch. */
    fun ensureChannel(context: Context) {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Payment capture",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Confirms expenses logged from Google Pay, and lets you pick a category."
            }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Posts (or updates) the capture notification. No-op if POST_NOTIFICATIONS isn't granted. */
    fun show(
        context: Context,
        transactionId: String,
        merchant: String,
        amountText: String,
        categoryName: String?,
        candidates: List<Category>,
    ) {
        ensureChannel(context)
        if (!hasPermission(context)) return

        val notificationId = transactionId.hashCode()
        val openEditor =
            PendingIntent.getActivity(
                context,
                notificationId,
                Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_EDIT_TRANSACTION_ID, transactionId)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val body = if (categoryName != null) "$merchant · $categoryName" else "$merchant · Pick a category"
        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_tally)
                .setContentTitle("Logged $amountText")
                .setContentText(body)
                .setContentIntent(openEditor)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (categoryName == null) {
            candidates.take(MAX_ACTIONS).forEach { category ->
                val select =
                    PendingIntent.getBroadcast(
                        context,
                        (transactionId + category.id).hashCode(),
                        CategorySelectionReceiver.selectionIntent(
                            context = context,
                            transactionId = transactionId,
                            categoryId = category.id,
                            categoryName = category.name,
                            merchant = merchant,
                            amountText = amountText,
                        ),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                builder.addAction(0, category.name, select)
            }
        } else {
            builder.addAction(0, "Change", openEditor)
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}
