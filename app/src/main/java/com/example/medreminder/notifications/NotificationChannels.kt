package com.example.medreminder.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.medreminder.R

object NotificationChannels {
    const val REMINDERS = "med_reminders"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(REMINDERS)
        if (existing != null) return
        val channel = NotificationChannel(
            REMINDERS,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            enableVibration(true)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun defaultSound(): Uri =
        android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
