package com.example.medreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.medreminder.ui.navigation.MedReminderApp
import com.example.medreminder.ui.permissions.ReminderPermissionsHost
import com.example.medreminder.ui.theme.MedReminderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as MedReminderApplication).repository
        enableEdgeToEdge()
        setContent {
            MedReminderTheme {
                ReminderPermissionsHost()
                MedReminderApp(repository = repository)
            }
        }
    }
}
