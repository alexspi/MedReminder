package com.example.medreminder.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: kotlinx.coroutines.flow.StateFlow<SettingsUiState>,
    onSelectTab: (SettingsTab) -> Unit,
    onFullNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
) {
    val uiState by state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
        PrimaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
            Tab(
                selected = uiState.selectedTab == SettingsTab.PERSONAL,
                onClick = { onSelectTab(SettingsTab.PERSONAL) },
                text = { Text(stringResource(R.string.settings_tab_personal)) },
            )
            Tab(
                selected = uiState.selectedTab == SettingsTab.UPDATE,
                onClick = { onSelectTab(SettingsTab.UPDATE) },
                text = { Text(stringResource(R.string.settings_tab_update)) },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (uiState.selectedTab) {
                SettingsTab.PERSONAL -> PersonalTab(
                    uiState = uiState,
                    onFullNameChange = onFullNameChange,
                    onAgeChange = onAgeChange,
                    onHeightChange = onHeightChange,
                    onWeightChange = onWeightChange,
                    onSaveProfile = onSaveProfile,
                )
                SettingsTab.UPDATE -> UpdateTab(
                    uiState = uiState,
                    onCheckUpdates = onCheckUpdates,
                    onDownloadUpdate = onDownloadUpdate,
                )
            }
        }
    }
}

@Composable
private fun PersonalTab(
    uiState: SettingsUiState,
    onFullNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_personal_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = uiState.fullName,
        onValueChange = onFullNameChange,
        label = { Text(stringResource(R.string.settings_full_name)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    OutlinedTextField(
        value = uiState.age,
        onValueChange = onAgeChange,
        label = { Text(stringResource(R.string.settings_age)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
    OutlinedTextField(
        value = uiState.heightCm,
        onValueChange = onHeightChange,
        label = { Text(stringResource(R.string.settings_height)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
    OutlinedTextField(
        value = uiState.weightKg,
        onValueChange = onWeightChange,
        label = { Text(stringResource(R.string.settings_weight)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
    if (uiState.saveMessage != null) {
        Text(
            text = uiState.saveMessage!!,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Button(
        onClick = onSaveProfile,
        enabled = !uiState.isSaving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (uiState.isSaving) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = 8.dp))
        }
        Text(stringResource(R.string.settings_save_profile))
    }
}

@Composable
private fun UpdateTab(
    uiState: SettingsUiState,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_version_label, uiState.appVersionName),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.settings_update_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(
        onClick = onCheckUpdates,
        enabled = !uiState.isCheckingUpdate && !uiState.isDownloadingUpdate,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (uiState.isCheckingUpdate) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = 8.dp))
        }
        Text(stringResource(R.string.settings_check_updates))
    }
    if (uiState.availableUpdate != null) {
        Text(
            text = stringResource(
                R.string.settings_update_available,
                uiState.availableUpdate!!.versionName,
            ),
            style = MaterialTheme.typography.titleSmall,
        )
        uiState.availableUpdate!!.releaseNotes?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onDownloadUpdate,
            enabled = !uiState.isDownloadingUpdate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isDownloadingUpdate) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = 8.dp))
            }
            val progress = uiState.downloadProgress
            val label = if (uiState.isDownloadingUpdate && progress != null) {
                stringResource(R.string.settings_downloading_update, (progress * 100).toInt())
            } else {
                stringResource(R.string.settings_download_install)
            }
            Text(label)
        }
    }
    if (uiState.updateMessage != null) {
        Text(
            text = uiState.updateMessage!!,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
