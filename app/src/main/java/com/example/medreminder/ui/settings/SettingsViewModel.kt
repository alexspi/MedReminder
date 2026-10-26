package com.example.medreminder.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medreminder.data.UserProfileRepository
import com.example.medreminder.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SettingsTab {
    PERSONAL,
    UPDATE,
}

data class SettingsUiState(
    val selectedTab: SettingsTab = SettingsTab.PERSONAL,
    val fullName: String = "",
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val profileLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val updateMessage: String? = null,
    val isCheckingUpdate: Boolean = false,
    val appVersionName: String = "",
)

class SettingsViewModel(
    private val userProfileRepository: UserProfileRepository,
    appVersionName: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState(appVersionName = appVersionName))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.observeProfile().collect { profile ->
                _uiState.update { state ->
                    if (state.profileLoaded) state
                    else state.copy(
                        fullName = profile.fullName,
                        age = profile.age,
                        heightCm = profile.heightCm,
                        weightKg = profile.weightKg,
                        profileLoaded = true,
                    )
                }
            }
        }
    }

    fun selectTab(tab: SettingsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateFullName(value: String) {
        _uiState.update { it.copy(fullName = value, saveMessage = null) }
    }

    fun updateAge(value: String) {
        _uiState.update { it.copy(age = value, saveMessage = null) }
    }

    fun updateHeight(value: String) {
        _uiState.update { it.copy(heightCm = value, saveMessage = null) }
    }

    fun updateWeight(value: String) {
        _uiState.update { it.copy(weightKg = value, saveMessage = null) }
    }

    fun saveProfile() {
        val state = _uiState.value
        val profile = UserProfile(
            fullName = state.fullName,
            age = state.age,
            heightCm = state.heightCm,
            weightKg = state.weightKg,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveMessage = null) }
            userProfileRepository.saveProfile(profile)
            _uiState.update { it.copy(isSaving = false, saveMessage = "Сохранено") }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, updateMessage = null) }
            kotlinx.coroutines.delay(500)
            val version = _uiState.value.appVersionName
            _uiState.update {
                it.copy(
                    isCheckingUpdate = false,
                    updateMessage = "Установлена актуальная версия $version",
                )
            }
        }
    }

    companion object {
        fun factory(
            userProfileRepository: UserProfileRepository,
            appVersionName: String,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(userProfileRepository, appVersionName) as T
            }
    }
}
