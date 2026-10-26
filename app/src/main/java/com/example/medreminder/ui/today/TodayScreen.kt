package com.example.medreminder.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.example.medreminder.ui.components.DoseActions
import com.example.medreminder.ui.components.DoseItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: kotlinx.coroutines.flow.StateFlow<TodayUiState>,
    onAddCourse: () -> Unit,
    onMarkTaken: (Long) -> Unit,
    onMarkSkipped: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
) {
    val uiState by state.collectAsStateWithLifecycle()
    val actions = DoseActions(onMarkTaken, onMarkSkipped, onSnooze)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.today_title)) })
        if (uiState.doses.isEmpty() && !uiState.isLoading) {
            EmptyToday(onAddCourse = onAddCourse)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.doses, key = { it.dose.id }) { item ->
                    DoseItemCard(item = item, actions = actions)
                }
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
