package com.example.medreminder.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.medreminder.MainActivity
import com.example.medreminder.R
import com.example.medreminder.domain.model.DoseWithCourse

object ReminderNotifier {
    fun show(context: Context, item: DoseWithCourse) {
        NotificationChannels.ensureCreated(context)
        if (!NotificationChannels.canPost(context)) return

        val doseId = item.dose.id
        val openApp = PendingIntent.getActivity(
            context,
            requestCode(doseId, 0),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val taken = actionPendingIntent(context, doseId, ReminderActions.MARK_TAKEN, 1)
        val skipped = actionPendingIntent(context, doseId, ReminderActions.MARK_SKIPPED, 2)
        val snooze = actionPendingIntent(context, doseId, ReminderActions.SNOOZE, 3)

        val sound = item.course.soundUri?.let(Uri::parse)
            ?: NotificationChannels.defaultSound()

        val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(item.course.name)
            .setContentText(
                context.getString(R.string.notification_body, item.course.dosage),
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    buildString {
                        append(item.course.dosage)
                        if (item.course.notes.isNotBlank()) {
                            append("\n")
                            append(item.course.notes)
                        }
                    },
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setSound(sound)
            .addAction(0, context.getString(R.string.action_taken), taken)
            .addAction(0, context.getString(R.string.action_skip), skipped)
            .addAction(0, context.getString(R.string.action_snooze), snooze)
            .build()

        NotificationManagerCompat.from(context).notify(notifyId(doseId), notification)
    }

    fun cancel(context: Context, doseId: Long) {
        NotificationManagerCompat.from(context).cancel(notifyId(doseId))
    }

    private fun actionPendingIntent(
        context: Context,
        doseId: Long,
        action: String,
        salt: Int,
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderIntentExtras.DOSE_ID, doseId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(doseId, salt),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notifyId(doseId: Long): Int = requestCode(doseId, 99)

    private fun requestCode(doseId: Long, salt: Int): Int =
        ((doseId xor salt.toLong()) and 0x7FFFFFFF).toInt()
}
