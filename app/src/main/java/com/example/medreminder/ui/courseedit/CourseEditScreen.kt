package com.example.medreminder.ui.courseedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medreminder.R
import com.example.medreminder.domain.model.DrugReferenceOption
import com.example.medreminder.domain.model.SavedDrugReference
import com.example.medreminder.ui.components.StartDatePickerField
import com.example.medreminder.ui.components.TimeOfDayPickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    state: kotlinx.coroutines.flow.StateFlow<CourseEditUiState>,
    onFieldChange: ((CourseEditUiState) -> CourseEditUiState) -> Unit,
    onAddTime: () -> Unit,
    onRemoveTime: (Int) -> Unit,
    onSoundSelected: (String?) -> Unit,
    onLoadGrls: () -> Unit,
    onSelectReference: (String) -> Unit,
    onSaveReference: () -> Unit,
    onClearGrls: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by state.collectAsStateWithLifecycle()
    val isNew = uiState.courseId == 0L

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        onSoundSelected(uri?.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) stringResource(R.string.course_edit_new)
                        else stringResource(R.string.course_edit_title),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { value -> onFieldChange { it.copy(name = value) } },
                label = { Text(stringResource(R.string.field_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.dosage,
                onValueChange = { value -> onFieldChange { it.copy(dosage = value) } },
                label = { Text(stringResource(R.string.field_dosage)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            StartDatePickerField(
                isoDate = uiState.startDateIso,
                onIsoDateChange = { value -> onFieldChange { it.copy(startDateIso = value) } },
                label = { Text(stringResource(R.string.field_start_date)) },
            )
            OutlinedTextField(
                value = uiState.durationDays,
                onValueChange = { value -> onFieldChange { it.copy(durationDays = value) } },
                label = { Text(stringResource(R.string.field_duration_days)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Text(
                text = stringResource(R.string.field_times),
                style = MaterialTheme.typography.titleSmall,
            )
            uiState.times.forEachIndexed { index, time ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TimeOfDayPickerField(
                        value = time,
                        onValueChange = { value ->
                            onFieldChange { state ->
                                state.copy(times = state.times.toMutableList().also { it[index] = value })
                            }
                        },
                        label = { Text(stringResource(R.string.field_time)) },
                        modifier = Modifier.weight(1f),
                    )
                    if (uiState.times.size > 1) {
                        IconButton(onClick = { onRemoveTime(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_time))
                        }
                    }
                }
            }
            OutlinedButton(onClick = onAddTime) {
                Text(stringResource(R.string.add_time))
            }

            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        uiState.soundUri?.let {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(it))
                        }
                    }
                    ringtonePicker.launch(intent)
                },
            ) {
                Text(
                    if (uiState.soundUri == null) {
                        stringResource(R.string.pick_sound_default)
                    } else {
                        stringResource(R.string.pick_sound_custom)
                    },
                )
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { value -> onFieldChange { it.copy(notes = value) } },
                label = { Text(stringResource(R.string.field_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Text(
                text = stringResource(R.string.grls_section_title),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedButton(
                onClick = onLoadGrls,
                enabled = !uiState.grlsLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.grlsLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(stringResource(R.string.grls_load_button))
            }
            if (uiState.savedReference != null) {
                SavedReferenceCard(reference = uiState.savedReference!!)
            }
            if (uiState.grlsError != null) {
                Text(
                    text = uiState.grlsError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (uiState.referenceNotice != null) {
                Text(
                    text = uiState.referenceNotice!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (uiState.referenceSourceLabel != null && uiState.referenceOptions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.grls_source, uiState.referenceSourceLabel!!),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            uiState.referenceOptions.forEach { option ->
                ReferenceOptionCard(
                    option = option,
                    selected = option.id == uiState.selectedOptionId,
                    onSelect = { onSelectReference(option.id) },
                )
            }
            if (uiState.referenceOptions.isNotEmpty()) {
                Button(
                    onClick = onSaveReference,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.grls_save_selection))
                }
            }
            if (uiState.savedReference != null || uiState.referenceOptions.isNotEmpty()) {
                TextButton(onClick = onClearGrls) {
                    Text(stringResource(R.string.grls_clear))
                }
            }
            Text(
                text = stringResource(R.string.grls_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = onSave,
                enabled = uiState.isLoaded && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_course))
            }
        }
    }
}

@Composable
private fun SavedReferenceCard(reference: SavedDrugReference) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.grls_saved_in_course, reference.displayLine()),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!reference.status.isNullOrBlank()) {
                Text(
                    text = reference.status!!,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ReferenceOptionCard(
    option: DrugReferenceOption,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(text = option.tradeName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "МНН: ${option.innName}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Статус: ${option.status}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!option.registrationNumber.isNullOrBlank()) {
                    Text(
                        text = "РУ: ${option.registrationNumber}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (option.dosageFormsSummary.isNotBlank()) {
                    Text(
                        text = option.dosageFormsSummary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
