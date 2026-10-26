package com.example.medreminder.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medreminder.data.MedReminderRepository
import com.example.medreminder.domain.model.DoseWithCourse
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HistoryDayGroup(
    val date: LocalDate,
    val items: List<DoseWithCourse>,
)

data class HistoryUiState(
    val groups: List<HistoryDayGroup> = emptyList(),
)

class HistoryViewModel(
    repository: MedReminderRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> =
        repository.observeWeekHistory()
            .map { doses ->
                val grouped = doses
                    .groupBy { it.dose.scheduledAt.toLocalDate() }
                    .toSortedMap(compareByDescending { it })
                    .map { (date, items) ->
                        HistoryDayGroup(date, items.sortedBy { it.dose.scheduledAt })
                    }
                HistoryUiState(groups = grouped)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    companion object {
        fun factory(repository: MedReminderRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HistoryViewModel(repository) as T
            }
    }
}
