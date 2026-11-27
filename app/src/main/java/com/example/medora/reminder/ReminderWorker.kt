package com.example.medora.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medora.MainActivity
import com.example.medora.R
import com.example.medora.data.MedoraDatabase

// Runs once a day. Sends a notification only if there are cards due for review.
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!ReminderScheduler.isEnabled(applicationContext)) return Result.success()
            val dueCount = MedoraDatabase.get(applicationContext)
                .cardDao()
                .getDue(System.currentTimeMillis(), 999)
                .size
            if (dueCount > 0) notifyDue(dueCount)
            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }

    private fun notifyDue(count: Int) {
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return
        ReminderScheduler.ensureChannel(applicationContext)
        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.reminder_notif_title))
            .setContentText(applicationContext.getString(R.string.reminder_notif_text, count))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(1001, notification)
        } catch (e: SecurityException) {
            // Notification permission was removed; ignore.
        }
    }
}
