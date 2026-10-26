package com.example.medreminder.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.medreminder.R
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.util.Locale

fun formatTimeOfDay(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

fun parseTimeOfDayOrDefault(raw: String, defaultHour: Int = 8, defaultMinute: Int = 0): Pair<Int, Int> {
    return try {
        val time = LocalTime.parse(raw.trim())
        time.hour to time.minute
    } catch (_: DateTimeParseException) {
        defaultHour to defaultMinute
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeOfDayPickerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val openPicker = { showPicker = true }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = label,
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .clickable(onClick = openPicker),
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = openPicker) {
                Icon(Icons.Default.Schedule, contentDescription = stringResource(R.string.field_time))
            }
        },
    )

    if (showPicker) {
        val (initialHour, initialMinute) = parseTimeOfDayOrDefault(value)
        val pickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
        )
        Dialog(onDismissRequest = { showPicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.field_time),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TimeInput(state = pickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showPicker = false }) {
                            Text(stringResource(R.string.time_picker_cancel))
                        }
                        TextButton(
                            onClick = {
                                onValueChange(formatTimeOfDay(pickerState.hour, pickerState.minute))
                                showPicker = false
                            },
                        ) {
                            Text(stringResource(R.string.time_picker_confirm))
                        }
                    }
                }
            }
        }
    }
}
