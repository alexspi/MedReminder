package com.example.medreminder.ui.today

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medreminder.data.MedReminderRepository
import com.example.medreminder.domain.model.StockAlert
import com.example.medreminder.notifications.ReminderNotifier
import com.example.medreminder.notifications.StockAlertNotifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val doses: List<com.example.medreminder.domain.model.DoseWithCourse> = emptyList(),
    val stockAlerts: List<StockAlert> = emptyList(),
    val isLoading: Boolean = true,
)

class TodayViewModel(
    private val repository: MedReminderRepository,
    private val application: Application,
) : ViewModel() {
    val uiState: StateFlow<TodayUiState> =
        combine(
            repository.observeTodayDoses(),
            repository.observeStockAlerts(),
        ) { doses, alerts ->
            TodayUiState(doses = doses, stockAlerts = alerts, isLoading = false)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TodayUiState(),
        )

    init {
        refreshStockNotifications()
    }

    fun refreshStockNotifications() {
        viewModelScope.launch {
            val list = repository.observeCourses().first()
            StockAlertNotifier.notifyAllLowStock(application, list)
        }
    }

    fun markTaken(doseId: Long) = updateDose(doseId) {
        val updatedCourse = repository.markTaken(doseId)
        if (updatedCourse != null) {
            StockAlertNotifier.notifyIfNeeded(application, updatedCourse)
        }
    }

    fun markSkipped(doseId: Long) = updateDose(doseId) { repository.markSkipped(doseId) }

    fun snooze(doseId: Long) = updateDose(doseId) { repository.snooze(doseId) }

    private fun updateDose(doseId: Long, block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            ReminderNotifier.cancel(application, doseId)
        }
    }

    companion object {
        fun factory(repository: MedReminderRepository, application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TodayViewModel(repository, application) as T
            }
    }
}
