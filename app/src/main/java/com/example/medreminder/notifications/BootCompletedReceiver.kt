package com.example.medreminder.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medreminder.MedReminderApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }
        val pending = goAsync()
        val app = context.applicationContext as MedReminderApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.scheduler.rescheduleAll(app.repository)
            } finally {
                pending.finish()
            }
        }
    }
}
