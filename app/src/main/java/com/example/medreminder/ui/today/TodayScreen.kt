package com.example.medreminder.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medreminder.R
import com.example.medreminder.domain.model.StockAlert
import com.example.medreminder.ui.components.DoseActions
import com.example.medreminder.ui.components.DoseItemCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: kotlinx.coroutines.flow.StateFlow<TodayUiState>,
    onAddCourse: () -> Unit,
    onEditCourse: (Long) -> Unit,
    onMarkTaken: (Long) -> Unit,
    onMarkSkipped: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
) {
    val uiState by state.collectAsStateWithLifecycle()
    val actions = DoseActions(onMarkTaken, onMarkSkipped, onSnooze)
    val todayDateLabel = LocalDate.now().format(
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru")),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.today_title)) },
            actions = {
                Text(
                    text = todayDateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp),
                )
            },
        )
        if (uiState.doses.isEmpty() && uiState.stockAlerts.isEmpty() && !uiState.isLoading) {
            EmptyToday(onAddCourse = onAddCourse)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.stockAlerts.isNotEmpty()) {
                    item(key = "stock_alerts_header") {
                        Text(
                            text = stringResource(R.string.stock_home_section_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    items(uiState.stockAlerts, key = { "stock_${it.courseId}" }) { alert ->
                        StockAlertCard(alert = alert, onEditCourse = onEditCourse)
                    }
                }
                items(uiState.doses, key = { it.dose.id }) { item ->
                    DoseItemCard(item = item, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun StockAlertCard(
    alert: StockAlert,
    onEditCourse: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (alert.depleted) {
                    stringResource(R.string.stock_home_depleted, alert.courseName)
                } else {
                    stringResource(
                        R.string.stock_home_low,
                        alert.courseName,
                        alert.remainingUnits,
                        alert.daysOfSupplyLeft,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(R.string.stock_home_action_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            OutlinedButton(onClick = { onEditCourse(alert.courseId) }) {
                Text(stringResource(R.string.stock_home_refill_button))
            }
        }
    }
}

@Composable
private fun EmptyToday(onAddCourse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.today_empty),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onAddCourse,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.add_first_course))
        }
    }
}
