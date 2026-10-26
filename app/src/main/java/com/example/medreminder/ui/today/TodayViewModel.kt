package com.example.medreminder.ui.today

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medreminder.data.MedReminderRepository
import com.example.medreminder.domain.model.DoseWithCourse
import com.example.medreminder.notifications.ReminderNotifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val doses: List<DoseWithCourse> = emptyList(),
    val isLoading: Boolean = true,
)

class TodayViewModel(
    private val repository: MedReminderRepository,
    private val application: Application,
) : ViewModel() {
    val uiState: StateFlow<TodayUiState> =
        repository.observeTodayDoses()
            .map { doses ->
                TodayUiState(doses = doses, isLoading = false)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TodayUiState(),
            )

    fun markTaken(doseId: Long) = updateDose(doseId) { repository.markTaken(doseId) }

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
