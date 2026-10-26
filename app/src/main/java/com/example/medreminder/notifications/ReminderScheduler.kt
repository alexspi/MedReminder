package com.example.medreminder.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.medreminder.data.MedReminderRepository
import com.example.medreminder.domain.model.DoseEvent

class ReminderScheduler(
    private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager.canScheduleExactAlarms()
    }

    fun schedule(dose: DoseEvent) {
        val triggerAt = dose.scheduledAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerAt <= System.currentTimeMillis()) return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderActions.ALARM_FIRE
            putExtra(ReminderIntentExtras.DOSE_ID, dose.id)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(dose.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    fun cancel(doseId: Long) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderActions.ALARM_FIRE
            putExtra(ReminderIntentExtras.DOSE_ID, doseId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(doseId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pending)
    }

    suspend fun rescheduleAll(repository: MedReminderRepository) {
        val pending = repository.getPendingDosesForScheduling()
        for (dose in pending) {
            cancel(dose.id)
            schedule(dose)
        }
    }

    private fun requestCode(doseId: Long): Int = (doseId and 0x7FFFFFFF).toInt()
}
