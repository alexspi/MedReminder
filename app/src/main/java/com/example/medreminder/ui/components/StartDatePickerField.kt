package com.example.medreminder.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import com.example.medreminder.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

fun formatStartDateForDisplay(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate.trim())
        String.format("%02d.%02d.%04d", date.dayOfMonth, date.monthValue, date.year)
    } catch (_: DateTimeParseException) {
        isoDate
    }
}

fun isoDateFromMillis(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

fun millisFromIsoDate(isoDate: String): Long? {
    return try {
        LocalDate.parse(isoDate.trim())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartDatePickerField(
    isoDate: String,
    onIsoDateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val openPicker = { showPicker = true }
    val displayValue = formatStartDateForDisplay(isoDate)

    OutlinedTextField(
        value = displayValue,
        onValueChange = {},
        readOnly = true,
        label = label,
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .clickable(onClick = openPicker),
        singleLine = true,
        supportingText = { Text(stringResource(R.string.field_start_date_hint)) },
        trailingIcon = {
            IconButton(onClick = openPicker) {
                Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.field_start_date))
            }
        },
    )

    if (showPicker) {
        val initialMillis = millisFromIsoDate(isoDate)
            ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onIsoDateChange(isoDateFromMillis(millis))
                        }
                        showPicker = false
                    },
                ) {
                    Text(stringResource(R.string.time_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.time_picker_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
