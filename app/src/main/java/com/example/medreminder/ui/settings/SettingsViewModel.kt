package com.example.medreminder.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medreminder.data.UserProfileRepository
import com.example.medreminder.domain.model.UserProfile
import com.example.medreminder.update.ApkInstaller
import com.example.medreminder.update.AvailableUpdate
import com.example.medreminder.update.GitHubAppUpdateRepository
import com.example.medreminder.update.UpdateCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SettingsTab {
    PERSONAL,
    UPDATE,
}

data class AvailableUpdateUi(
    val versionName: String,
    val releaseNotes: String?,
)

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
    val isDownloadingUpdate: Boolean = false,
    val downloadProgress: Float? = null,
    val availableUpdate: AvailableUpdateUi? = null,
    val appVersionName: String = "",
)

class SettingsViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val appUpdateRepository: GitHubAppUpdateRepository,
    private val installedVersionCode: Int,
    appVersionName: String,
    private val installContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState(appVersionName = appVersionName))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var pendingUpdate: AvailableUpdate? = null

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
            _uiState.update {
                it.copy(
                    isCheckingUpdate = true,
                    updateMessage = null,
                    availableUpdate = null,
                    downloadProgress = null,
                )
            }
            pendingUpdate = null

            when (val result = appUpdateRepository.checkForUpdate(installedVersionCode)) {
                UpdateCheckResult.NotConfigured -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateMessage = "Укажите medreminder.githubOwner в gradle.properties",
                        )
                    }
                }
                UpdateCheckResult.UpToDate -> {
                    val version = _uiState.value.appVersionName
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateMessage = "Установлена актуальная версия $version",
                        )
                    }
                }
                is UpdateCheckResult.Available -> {
                    pendingUpdate = result.update
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            availableUpdate = AvailableUpdateUi(
                                versionName = result.update.versionName,
                                releaseNotes = result.update.releaseNotes,
                            ),
                            updateMessage = "Доступна версия ${result.update.versionName}",
                        )
                    }
                }
                is UpdateCheckResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val update = pendingUpdate ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloadingUpdate = true,
                    downloadProgress = 0f,
                    updateMessage = null,
                )
            }
            try {
                val apkFile = appUpdateRepository.downloadApk(
                    downloadUrl = update.apkDownloadUrl,
                    onProgress = { progress ->
                        _uiState.update { it.copy(downloadProgress = progress) }
                    },
                )
                if (!ApkInstaller.canInstallPackages(installContext)) {
                    ApkInstaller.openUnknownSourcesSettings(installContext)
                    _uiState.update {
                        it.copy(
                            isDownloadingUpdate = false,
                            downloadProgress = null,
                            updateMessage = "Разрешите установку из этого приложения и нажмите снова",
                        )
                    }
                    return@launch
                }
                val started = ApkInstaller.install(installContext, apkFile)
                _uiState.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        downloadProgress = null,
                        updateMessage = if (started) {
                            "Откроется установщик Android"
                        } else {
                            "Не удалось запустить установку"
                        },
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        downloadProgress = null,
                        updateMessage = e.message ?: "Ошибка скачивания",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            userProfileRepository: UserProfileRepository,
            appUpdateRepository: GitHubAppUpdateRepository,
            installedVersionCode: Int,
            appVersionName: String,
            installContext: Context,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(
                        userProfileRepository,
                        appUpdateRepository,
                        installedVersionCode,
                        appVersionName,
                        installContext,
                    ) as T
            }
    }
}
