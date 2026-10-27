package com.example.medreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.medreminder.notifications.StockAlertNotifier
import com.example.medreminder.ui.navigation.MedReminderApp
import com.example.medreminder.ui.permissions.ReminderPermissionsHost
import com.example.medreminder.ui.theme.MedReminderTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MedReminderApplication
        enableEdgeToEdge()
        setContent {
            MedReminderTheme {
                ReminderPermissionsHost()
                MedReminderApp(repository = app.repository)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as MedReminderApplication
        lifecycleScope.launch {
            if (app.scheduler.canScheduleExactAlarms()) {
                app.scheduler.rescheduleAll(app.repository)
            }
            val courses = app.repository.observeCourses().first()
            StockAlertNotifier.notifyAllLowStock(app, courses)
        }
    }
}
