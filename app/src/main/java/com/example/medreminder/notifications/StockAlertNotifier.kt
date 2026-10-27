package com.example.medreminder.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.medreminder.MainActivity
import com.example.medreminder.R
import com.example.medreminder.domain.StockSupplyCalculator
import com.example.medreminder.domain.model.MedicationCourse

object StockAlertNotifier {
    fun notifyIfNeeded(context: Context, course: MedicationCourse) {
        if (!StockSupplyCalculator.tracksInventory(course)) return
        if (!StockSupplyCalculator.isLowSupply(course) &&
            !StockSupplyCalculator.isDepleted(course)
        ) {
            return
        }
        NotificationChannels.ensureStockChannel(context)
        if (!NotificationChannels.canPost(context)) return

        val daysLeft = StockSupplyCalculator.daysOfSupplyRemaining(course)
        val depleted = StockSupplyCalculator.isDepleted(course)
        val title = if (depleted) {
            context.getString(R.string.stock_notification_depleted_title, course.name)
        } else {
            context.getString(R.string.stock_notification_low_title, course.name)
        }
        val text = if (depleted) {
            context.getString(R.string.stock_notification_depleted_body)
        } else {
            context.getString(
                R.string.stock_notification_low_body,
                course.remainingUnits,
                daysLeft ?: 0,
            )
        }

        val openApp = PendingIntent.getActivity(
            context,
            notifyId(course.id),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.STOCK)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSound(NotificationChannels.defaultSound())
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setContentIntent(openApp)
            .build()

        NotificationManagerCompat.from(context).notify(notifyId(course.id), notification)
    }

    fun notifyAllLowStock(context: Context, courses: List<MedicationCourse>) {
        courses.forEach { notifyIfNeeded(context, it) }
    }

    private fun notifyId(courseId: Long): Int = (courseId xor 0x5A5A5A5AL).toInt()
}
