package com.example.medreminder.ui.courseedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medreminder.data.DrugReferenceRepository
import com.example.medreminder.data.MedReminderRepository
import com.example.medreminder.domain.model.DrugReferenceOption
import com.example.medreminder.domain.model.MedicationCourse
import com.example.medreminder.domain.model.SavedDrugReference
import com.example.medreminder.domain.model.toSaved
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

data class CourseEditUiState(
    val courseId: Long = 0,
    val name: String = "",
    val dosage: String = "",
    val durationDays: String = "7",
    val startDateIso: String = LocalDate.now().toString(),
    val times: List<String> = listOf("08:00"),
    val notes: String = "",
    val soundUri: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isLoaded: Boolean = false,
    val grlsLoading: Boolean = false,
    val grlsError: String? = null,
    val referenceOptions: List<DrugReferenceOption> = emptyList(),
    val referenceNotice: String? = null,
    val referenceSourceLabel: String? = null,
    val selectedOptionId: String? = null,
    val savedReference: SavedDrugReference? = null,
)

class CourseEditViewModel(
    private val repository: MedReminderRepository,
    private val drugReferenceRepository: DrugReferenceRepository,
    private val courseId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CourseEditUiState(courseId = courseId))
    val uiState: StateFlow<CourseEditUiState> = _uiState.asStateFlow()

    init {
        if (courseId > 0) {
            viewModelScope.launch {
                val course = repository.getCourse(courseId)
                if (course != null) {
                    _uiState.update {
                        it.copy(
                            name = course.name,
                            dosage = course.dosage,
                            durationDays = course.durationDays.toString(),
                            startDateIso = course.startDate.toString(),
                            times = course.timesOfDay.map { time -> time.toString() },
                            notes = course.notes,
                            soundUri = course.soundUri,
                            savedReference = course.savedReference,
                            selectedOptionId = course.savedReference?.optionId,
                            isLoaded = true,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoaded = true, errorMessage = "Курс не найден") }
                }
            }
        } else {
            _uiState.update { it.copy(isLoaded = true) }
        }
    }

    fun updateField(block: (CourseEditUiState) -> CourseEditUiState) {
        _uiState.update(block)
    }

    fun addTimeSlot() {
        _uiState.update { state ->
            state.copy(times = state.times + "12:00")
        }
    }

    fun setSoundUri(uri: String?) {
        _uiState.update { it.copy(soundUri = uri) }
    }

    fun removeTimeSlot(index: Int) {
        _uiState.update { state ->
            if (state.times.size <= 1) state else state.copy(times = state.times.filterIndexed { i, _ -> i != index })
        }
    }

    fun loadGrlsReference() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(grlsError = "Сначала укажите название лекарства") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    grlsLoading = true,
                    grlsError = null,
                    referenceOptions = emptyList(),
                    referenceNotice = null,
                    referenceSourceLabel = null,
                )
            }
            try {
                val result = drugReferenceRepository.fetchDrugReferenceOptions(name)
                val preselect = result.options.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        grlsLoading = false,
                        referenceOptions = result.options,
                        referenceNotice = result.notice,
                        referenceSourceLabel = result.sourceLabel,
                        selectedOptionId = it.savedReference?.optionId ?: preselect,
                        grlsError = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        grlsLoading = false,
                        grlsError = e.message ?: "Не удалось загрузить справку",
                    )
                }
            }
        }
    }

    fun selectReferenceOption(optionId: String) {
        _uiState.update { it.copy(selectedOptionId = optionId) }
    }

    fun saveSelectedReference() {
        val state = _uiState.value
        val option = state.referenceOptions.find { it.id == state.selectedOptionId }
        if (option == null) {
            _uiState.update { it.copy(grlsError = "Выберите вариант из списка") }
            return
        }
        val saved = option.toSaved()
        _uiState.update {
            it.copy(
                savedReference = saved,
                name = if (it.name.isBlank()) option.tradeName else it.name,
                grlsError = null,
            )
        }
    }

    fun clearReference() {
        _uiState.update {
            it.copy(
                referenceOptions = emptyList(),
                referenceNotice = null,
                referenceSourceLabel = null,
                selectedOptionId = null,
                savedReference = null,
                grlsError = null,
                grlsLoading = false,
            )
        }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        val parsed = parseCourse(state)
        if (parsed == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repository.saveCourse(parsed)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Ошибка сохранения") }
            }
        }
    }

    private fun parseCourse(state: CourseEditUiState): MedicationCourse? {
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Укажите название лекарства") }
            return null
        }
        if (state.dosage.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Укажите дозировку") }
            return null
        }
        val days = state.durationDays.toIntOrNull()
        if (days == null || days < 1) {
            _uiState.update { it.copy(errorMessage = "Количество дней должно быть ≥ 1") }
            return null
        }
        val startDate = try {
            LocalDate.parse(state.startDateIso)
        } catch (_: DateTimeParseException) {
            _uiState.update { it.copy(errorMessage = "Дата начала в формате ГГГГ-ММ-ДД") }
            return null
        }
        val times = mutableListOf<LocalTime>()
        for (raw in state.times) {
            try {
                times += LocalTime.parse(raw.trim())
            } catch (_: DateTimeParseException) {
                _uiState.update { it.copy(errorMessage = "Время в формате ЧЧ:ММ") }
                return null
            }
        }
        if (times.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Добавьте хотя бы одно время приёма") }
            return null
        }
        return MedicationCourse(
            id = state.courseId,
            name = state.name,
            dosage = state.dosage,
            startDate = startDate,
            durationDays = days,
            timesOfDay = times,
            notes = state.notes,
            soundUri = state.soundUri,
            isActive = true,
            savedReference = state.savedReference,
        )
    }

    companion object {
        fun factory(
            repository: MedReminderRepository,
            drugReferenceRepository: DrugReferenceRepository,
            courseId: Long,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CourseEditViewModel(repository, drugReferenceRepository, courseId) as T
            }
    }
}
