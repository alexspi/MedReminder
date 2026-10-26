package com.example.medreminder.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medreminder.data.MedReminderRepository
import com.example.medreminder.domain.model.MedicationCourse
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CoursesUiState(
    val active: List<MedicationCourse> = emptyList(),
    val completed: List<MedicationCourse> = emptyList(),
)

class CoursesViewModel(
    repository: MedReminderRepository,
) : ViewModel() {
    val uiState: StateFlow<CoursesUiState> =
        repository.observeCourses()
            .map { courses ->
                CoursesUiState(
                    active = courses.filter { it.isActive },
                    completed = courses.filter { !it.isActive },
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CoursesUiState(),
            )

    companion object {
        fun factory(repository: MedReminderRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CoursesViewModel(repository) as T
            }
    }
}
