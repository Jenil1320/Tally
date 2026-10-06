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
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.tally.MainActivity
import com.tally.R
import com.tally.data.notify.CategorySelectionReceiver
import com.tally.data.notify.ShareSelectionReceiver
import com.tally.domain.insight.EATING_OUT_CATEGORY_NAME
import com.tally.domain.model.Category

/** What the share part of a capture notification shows. */
sealed interface CaptureShare {
    /** Offer the share actions when the category is Eating out, and nothing otherwise. */
    data object Auto : CaptureShare

    /** The share is saved. [text] is the formatted amount. Shows the entry as confirmed. */
    data class Saved(val text: String) : CaptureShare

    /** The last reply could not be used. Shows [hint] and offers the share actions again. */
    data class Retry(val hint: String) : CaptureShare
}

/**
 * Posts the notification shown after an expense is auto-captured from a payment
 * notification (see ADR-0009). The shape depends on what is still unknown:
 *
 *  - **Categorised** — a quiet confirmation (`Logged £4.20 · Pret · Cafe`) with a
 *    single "Change" action that opens the entry for editing.
 *  - **Uncategorised** — the same line ending `· Pick a category`, plus up to
 *    three one-tap category buttons ([candidates]); tapping one is handled by
 *    [CategorySelectionReceiver] without opening the app. Tapping the body opens
 *    the full editor.
 *  - **Eating out, share not set** — the line ends `· Add your share`, with
 *    three actions: "Half", "My share" (type an amount in the notification
 *    itself) and "Change". [ShareSelectionReceiver] saves the share without
 *    opening the app, then this notification is posted again as a confirmation
 *    that shows the share (ADR-0010).
 *
 * Tone is observational per the constitution — no emoji, no exclamation marks.
 * The notification id is derived from the transaction id, so re-posting after a
 * choice updates the same notification in place.
 */
object CaptureNotifier {
    const val CHANNEL_ID = "tally-capture"
    const val EXTRA_EDIT_TRANSACTION_ID = "com.tally.extra.EDIT_TRANSACTION_ID"

    /** Key of the text typed into the "My share" reply box. */
    const val KEY_SHARE_REPLY = "com.tally.key.SHARE_REPLY"

    private const val MAX_ACTIONS = 3

    /** Idempotently creates the capture notification channel. Safe on every launch. */
    fun ensureChannel(context: Context) {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Payment capture",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Confirms expenses logged from Google Pay, and lets you pick a category or add your share."
            }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Posts (or updates) the capture notification. No-op if POST_NOTIFICATIONS isn't granted.
     *
     * @param share what the share part of the notification shows. [CaptureShare.Auto]
     *   offers the share actions for Eating out only.
     */
    fun show(
        context: Context,
        transactionId: String,
        merchant: String,
        amountText: String,
        categoryName: String?,
        candidates: List<Category>,
        share: CaptureShare = CaptureShare.Auto,
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

        val askShare = share !is CaptureShare.Auto || categoryName.equals(EATING_OUT_CATEGORY_NAME, ignoreCase = true)
        val needsShare = askShare && categoryName != null && share !is CaptureShare.Saved
        val body =
            when {
                categoryName == null -> "$merchant · Pick a category"
                share is CaptureShare.Saved -> "$merchant · $categoryName · Your share ${share.text}"
                askShare -> "$merchant · $categoryName · ${(share as? CaptureShare.Retry)?.hint ?: "Add your share"}"
                else -> "$merchant · $categoryName"
            }
        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_tally)
                .setContentTitle("Logged $amountText")
                .setContentText(body)
                .setContentIntent(openEditor)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        when {
            categoryName == null ->
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
            needsShare -> addShareActions(context, builder, transactionId, merchant, categoryName.orEmpty(), amountText, openEditor)
            else -> builder.addAction(0, "Change", openEditor)
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    /** "Half", "My share" (an inline reply box) and "Change". */
    private fun addShareActions(
        context: Context,
        builder: NotificationCompat.Builder,
        transactionId: String,
        merchant: String,
        categoryName: String,
        amountText: String,
        openEditor: PendingIntent,
    ) {
        val half =
            PendingIntent.getBroadcast(
                context,
                (transactionId + "half").hashCode(),
                ShareSelectionReceiver.halfIntent(context, transactionId, merchant, categoryName, amountText),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        builder.addAction(0, "Half", half)

        // The reply box needs a mutable PendingIntent so the system can add the typed text.
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val typed =
            PendingIntent.getBroadcast(
                context,
                (transactionId + "typed").hashCode(),
                ShareSelectionReceiver.typedIntent(context, transactionId, merchant, categoryName, amountText),
                PendingIntent.FLAG_UPDATE_CURRENT or mutable,
            )
        val reply = RemoteInput.Builder(KEY_SHARE_REPLY).setLabel("Your share, e.g. 12.50").build()
        builder.addAction(NotificationCompat.Action.Builder(0, "My share", typed).addRemoteInput(reply).build())

        builder.addAction(0, "Change", openEditor)
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}
