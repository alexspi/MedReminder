package com.example.medreminder.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medreminder.MedReminderApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getLongExtra(ReminderIntentExtras.DOSE_ID, -1L)
        if (doseId < 0) return
        val pending = goAsync()
        val app = context.applicationContext as MedReminderApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val item = app.repository.getDoseWithCourse(doseId)
                if (item != null && item.dose.status == com.example.medreminder.data.local.DoseStatus.PENDING) {
                    ReminderNotifier.show(context, item)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
