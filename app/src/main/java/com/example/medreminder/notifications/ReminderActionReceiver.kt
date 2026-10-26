package com.example.medreminder.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medreminder.MedReminderApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getLongExtra(ReminderIntentExtras.DOSE_ID, -1L)
        if (doseId < 0) return
        val pending = goAsync()
        val app = context.applicationContext as MedReminderApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderActions.MARK_TAKEN -> app.repository.markTaken(doseId)
                    ReminderActions.MARK_SKIPPED -> app.repository.markSkipped(doseId)
                    ReminderActions.SNOOZE -> app.repository.snooze(doseId)
                }
                ReminderNotifier.cancel(context, doseId)
            } finally {
                pending.finish()
            }
        }
    }
}
