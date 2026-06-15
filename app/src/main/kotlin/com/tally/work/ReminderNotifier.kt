package com.tally.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tally.MainActivity
import com.tally.R

/**
 * Builds and posts the daily reminder notification. Tone is observational — no
 * exclamation marks, no emoji, no streak/gamification (the constitution).
 */
object ReminderNotifier {
    const val CHANNEL_ID = "tally-reminder"
    private const val NOTIFICATION_ID = 2200

    /** Idempotently creates the notification channel. Safe to call on every launch. */
    fun ensureChannel(context: Context) {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Daily reminder",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "A nudge at your chosen time to log the day's spending."
            }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Posts the reminder, unless POST_NOTIFICATIONS is not granted (then no-op). */
    fun show(context: Context) {
        ensureChannel(context)
        if (!hasPermission(context)) return

        val openApp =
            android.app.PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                android.app.PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_tally)
                .setContentTitle("Today's spending")
                .setContentText("A quiet moment to log what you spent.")
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}
