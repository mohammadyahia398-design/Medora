package com.example.medora.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.medora.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

// Daily review reminder. Settings live in a small private preferences file.
object ReminderScheduler {
    const val CHANNEL_ID = "review_reminder"
    private const val WORK_NAME = "review_reminder_daily"
    private const val PREFS = "reminder_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour"
    private const val DEFAULT_HOUR = 20

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun getHour(context: Context): Int =
        prefs(context).getInt(KEY_HOUR, DEFAULT_HOUR).coerceIn(0, 23)

    fun enable(context: Context, hour: Int) {
        val h = hour.coerceIn(0, 23)
        prefs(context).edit().putBoolean(KEY_ENABLED, true).putInt(KEY_HOUR, h).apply()
        ensureChannel(context)
        schedule(context, h)
    }

    fun disable(context: Context) {
        prefs(context).edit().putBoolean(KEY_ENABLED, false).apply()
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }

    fun changeHour(context: Context, hour: Int) {
        val h = hour.coerceIn(0, 23)
        prefs(context).edit().putInt(KEY_HOUR, h).apply()
        if (isEnabled(context)) schedule(context, h)
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    private fun schedule(context: Context, hour: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
