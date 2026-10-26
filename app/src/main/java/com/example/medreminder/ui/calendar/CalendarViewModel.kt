package com.example.medreminder.ui.calendar

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medreminder.data.MedReminderRepository
import com.example.medreminder.notifications.ReminderNotifier
import com.example.medreminder.domain.model.DoseWithCourse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val datesWithDoses: Set<LocalDate> = emptySet(),
    val selectedDayDoses: List<DoseWithCourse> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val repository: MedReminderRepository,
    private val application: Application,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> =
        combine(month, selectedDate) { ym, date -> ym to date }
            .flatMapLatest { (ym, date) ->
                val monthStart = ym.atDay(1)
                val monthEnd = ym.plusMonths(1).atDay(1)
                combine(
                    repository.observeDosesInRange(monthStart, monthEnd),
                    repository.observeDosesForDate(date),
                ) { monthDoses, dayDoses ->
                    CalendarUiState(
                        month = ym,
                        selectedDate = date,
                        datesWithDoses = monthDoses.map { it.dose.scheduledAt.toLocalDate() }.toSet(),
                        selectedDayDoses = dayDoses,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun previousMonth() {
        month.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        month.update { it.plusMonths(1) }
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun markTaken(doseId: Long) = applyDoseAction(doseId) { repository.markTaken(doseId) }

    fun markSkipped(doseId: Long) = applyDoseAction(doseId) { repository.markSkipped(doseId) }

    fun snooze(doseId: Long) = applyDoseAction(doseId) { repository.snooze(doseId) }

    private fun applyDoseAction(doseId: Long, block: suspend () -> Unit) {
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
                    CalendarViewModel(repository, application) as T
            }
    }
}
