package com.example.medreminder.ui.courseedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.medreminder.R
import com.example.medreminder.domain.model.CourseKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCreateTypeScreen(
    onSelectKind: (CourseKind) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.course_create_type_title)) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.course_create_type_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            CreateTypeCard(
                title = stringResource(R.string.course_kind_medication),
                subtitle = stringResource(R.string.course_kind_medication_desc),
                icon = { Icon(Icons.Default.Medication, contentDescription = null) },
                onClick = { onSelectKind(CourseKind.MEDICATION) },
            )
            CreateTypeCard(
                title = stringResource(R.string.course_kind_supplement),
                subtitle = stringResource(R.string.course_kind_supplement_desc),
                icon = { Icon(Icons.Default.Spa, contentDescription = null) },
                onClick = { onSelectKind(CourseKind.SUPPLEMENT) },
            )
            CreateTypeCard(
                title = stringResource(R.string.course_kind_planned_action),
                subtitle = stringResource(R.string.course_kind_planned_action_desc),
                icon = { Icon(Icons.Default.Event, contentDescription = null) },
                onClick = { onSelectKind(CourseKind.PLANNED_ACTION) },
            )
        }
    }
}

@Composable
private fun CreateTypeCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            icon()
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
