package com.example.medreminder.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.medreminder.MainActivity
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
        val pending = alarmPendingIntent(dose.id)
        val showIntent = showPendingIntent(dose.id)
        try {
            if (canScheduleExactAlarms()) {
                val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
                alarmManager.setAlarmClock(info, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (security: SecurityException) {
            Log.w(TAG, "Exact alarm denied, using inexact fallback for dose ${dose.id}", security)
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule dose ${dose.id}", e)
            }
        }
    }

    fun cancel(doseId: Long) {
        alarmManager.cancel(alarmPendingIntent(doseId))
    }

    suspend fun rescheduleAll(repository: MedReminderRepository) {
        val pending = repository.getPendingDosesForScheduling()
        for (dose in pending) {
            cancel(dose.id)
            schedule(dose)
        }
    }

    private fun alarmPendingIntent(doseId: Long): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderActions.ALARM_FIRE
            putExtra(ReminderIntentExtras.DOSE_ID, doseId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(doseId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showPendingIntent(doseId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderIntentExtras.DOSE_ID, doseId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode(doseId) + SHOW_INTENT_SALT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(doseId: Long): Int = (doseId and 0x7FFFFFFF).toInt()

    companion object {
        private const val TAG = "ReminderScheduler"
        private const val SHOW_INTENT_SALT = 1_000_000
    }
}
