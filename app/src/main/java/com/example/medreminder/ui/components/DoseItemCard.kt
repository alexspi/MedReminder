package com.example.medreminder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medreminder.R
import com.example.medreminder.data.local.DoseStatus
import com.example.medreminder.domain.model.DoseWithCourse
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

data class DoseActions(
    val onMarkTaken: (Long) -> Unit,
    val onMarkSkipped: (Long) -> Unit,
    val onSnooze: (Long) -> Unit,
)

@Composable
fun DoseItemCard(
    item: DoseWithCourse,
    modifier: Modifier = Modifier,
    showTime: Boolean = true,
    actions: DoseActions? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (showTime) {
                Text(
                    text = timeFormatter.format(item.dose.scheduledAt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = item.course.name,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = if (showTime) 4.dp else 0.dp),
            )
            Text(
                text = item.course.dosage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.course.notes.isNotBlank()) {
                Text(
                    text = item.course.notes,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                text = doseStatusLabel(item.dose.status),
                style = MaterialTheme.typography.labelMedium,
                color = statusColor(item.dose.status),
                modifier = Modifier.padding(top = 8.dp),
            )
            if (actions != null &&
                (item.dose.status == DoseStatus.PENDING || item.dose.status == DoseStatus.SNOOZED)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { actions.onMarkTaken(item.dose.id) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_taken))
                    }
                    OutlinedButton(onClick = { actions.onSnooze(item.dose.id) }) {
                        Text(stringResource(R.string.action_snooze))
                    }
                }
                TextButton(onClick = { actions.onMarkSkipped(item.dose.id) }) {
                    Text(stringResource(R.string.action_skip))
                }
            }
        }
    }
}

@Composable
fun doseStatusLabel(status: DoseStatus): String =
    when (status) {
        DoseStatus.PENDING -> stringResource(R.string.status_pending)
        DoseStatus.TAKEN -> stringResource(R.string.status_taken)
        DoseStatus.SKIPPED -> stringResource(R.string.status_skipped)
        DoseStatus.SNOOZED -> stringResource(R.string.status_snoozed)
    }

@Composable
private fun statusColor(status: DoseStatus) =
    when (status) {
        DoseStatus.TAKEN -> MaterialTheme.colorScheme.primary
        DoseStatus.SKIPPED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
