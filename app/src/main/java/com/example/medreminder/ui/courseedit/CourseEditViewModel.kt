package com.example.medreminder.ui.courseedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medreminder.data.DrugReferenceRepository
import com.example.medreminder.data.MedReminderRepository
import com.example.medreminder.domain.model.CourseKind
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
    val courseKind: CourseKind = CourseKind.MEDICATION,
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
    val unitsInPackage: String = "",
    val unitsPerDose: String = "1",
    val remainingUnits: String = "",
)

class CourseEditViewModel(
    private val repository: MedReminderRepository,
    private val drugReferenceRepository: DrugReferenceRepository,
    private val courseId: Long,
    private val initialCourseKind: CourseKind,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CourseEditUiState(courseId = courseId, courseKind = initialCourseKind),
    )
    val uiState: StateFlow<CourseEditUiState> = _uiState.asStateFlow()

    init {
        if (courseId > 0) {
            viewModelScope.launch {
                val course = repository.getCourse(courseId)
                if (course != null) {
                    _uiState.update {
                        it.copy(
                            courseKind = course.courseKind,
                            name = course.name,
                            dosage = course.dosage,
                            durationDays = course.durationDays.toString(),
                            startDateIso = course.startDate.toString(),
                            times = course.timesOfDay.map { time -> time.toString() },
                            notes = course.notes,
                            soundUri = course.soundUri,
                            savedReference = course.savedReference,
                            selectedOptionId = course.savedReference?.optionId,
                            unitsInPackage = if (course.unitsInPackage > 0) course.unitsInPackage.toString() else "",
                            unitsPerDose = course.unitsPerDose.toString(),
                            remainingUnits = course.remainingUnits.toString(),
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

    fun updateUnitsInPackage(value: String) {
        _uiState.update { state ->
            val packageCount = value.trim().toIntOrNull()
            val remaining = when {
                state.courseId == 0L && packageCount != null && packageCount > 0 ->
                    packageCount.toString()
                state.courseId == 0L && value.isBlank() ->
                    ""
                else -> state.remainingUnits
            }
            state.copy(unitsInPackage = value, remainingUnits = remaining)
        }
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
        val state = _uiState.value
        if (!state.courseKind.showsDrugReference) return
        val name = state.name.trim()
        if (name.isBlank()) {
            val message = when (state.courseKind) {
                CourseKind.SUPPLEMENT -> "Сначала укажите название БАД"
                else -> "Сначала укажите название лекарства"
            }
            _uiState.update { it.copy(grlsError = message) }
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
                val result = when (state.courseKind) {
                    CourseKind.SUPPLEMENT -> drugReferenceRepository.fetchSupplementReferenceOptions(name)
                    CourseKind.MEDICATION -> drugReferenceRepository.fetchDrugReferenceOptions(name)
                    CourseKind.PLANNED_ACTION -> throw IllegalStateException()
                }
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
            val message = when (state.courseKind) {
                CourseKind.PLANNED_ACTION -> "Укажите название действия"
                CourseKind.SUPPLEMENT -> "Укажите название БАД"
                CourseKind.MEDICATION -> "Укажите название лекарства"
            }
            _uiState.update { it.copy(errorMessage = message) }
            return null
        }
        val dosage = state.dosage.trim()
        if (state.courseKind.showsDosageAndStock && dosage.isBlank()) {
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
        val packageUnits: Int
        val perDose: Int
        val remainingUnits: Int
        if (state.courseKind.showsDosageAndStock) {
            packageUnits = state.unitsInPackage.trim().toIntOrNull() ?: 0
            val perDoseParsed = state.unitsPerDose.trim().toIntOrNull()
            if (packageUnits > 0) {
                if (perDoseParsed == null || perDoseParsed < 1) {
                    _uiState.update { it.copy(errorMessage = "Количество на приём должно быть ≥ 1") }
                    return null
                }
            }
            val remainingParsed = state.remainingUnits.trim().toIntOrNull()
            remainingUnits = when {
                packageUnits <= 0 -> 0
                remainingParsed != null -> remainingParsed.coerceIn(0, packageUnits)
                else -> packageUnits
            }
            if (packageUnits > 0 && remainingUnits > packageUnits) {
                _uiState.update { it.copy(errorMessage = "Остаток не может быть больше упаковки") }
                return null
            }
            perDose = (perDoseParsed ?: 1).coerceAtLeast(1)
        } else {
            packageUnits = 0
            perDose = 1
            remainingUnits = 0
        }
        return MedicationCourse(
            id = state.courseId,
            courseKind = state.courseKind,
            name = state.name,
            dosage = if (state.courseKind.showsDosageAndStock) dosage else "",
            startDate = startDate,
            durationDays = days,
            timesOfDay = times,
            notes = state.notes,
            soundUri = state.soundUri,
            isActive = true,
            savedReference = if (state.courseKind.showsDrugReference) state.savedReference else null,
            unitsInPackage = packageUnits,
            unitsPerDose = perDose,
            remainingUnits = remainingUnits,
        )
    }

    companion object {
        fun factory(
            repository: MedReminderRepository,
            drugReferenceRepository: DrugReferenceRepository,
            courseId: Long,
            initialCourseKind: CourseKind,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CourseEditViewModel(
                        repository,
                        drugReferenceRepository,
                        courseId,
                        initialCourseKind,
                    ) as T
            }
    }
}
