package com.example.medreminder.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.medreminder.MedReminderApplication
import com.example.medreminder.data.local.DoseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getLongExtra(ReminderIntentExtras.DOSE_ID, -1L)
        if (doseId < 0) return
        val pending = goAsync()
        val app = context.applicationContext as MedReminderApplication
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MedReminder:AlarmWake")
            .apply { setReferenceCounted(false) }
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val item = app.repository.getDoseWithCourse(doseId)
                if (item != null && item.dose.status == DoseStatus.PENDING) {
                    ReminderNotifier.show(context, item)
                }
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                pending.finish()
            }
        }
    }

    companion object {
        private const val WAKE_LOCK_TIMEOUT_MS = 15_000L
    }
}
