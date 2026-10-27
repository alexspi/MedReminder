package com.example.medreminder.ui.courses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medreminder.R
import com.example.medreminder.domain.model.CourseKind
import com.example.medreminder.domain.model.MedicationCourse
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ru"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    state: kotlinx.coroutines.flow.StateFlow<CoursesUiState>,
    onEditCourse: (Long, CourseKind) -> Unit,
) {
    val uiState by state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.medicines_title)) })
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.active.isEmpty() && uiState.completed.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.courses_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            if (uiState.active.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.courses_active),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(uiState.active, key = { it.id }) { course ->
                    CourseCard(course = course, onClick = { onEditCourse(course.id, course.courseKind) })
                }
            }
            if (uiState.completed.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.courses_completed),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(uiState.completed, key = { it.id }) { course ->
                    CourseCard(course = course, onClick = { onEditCourse(course.id, course.courseKind) })
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: MedicationCourse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = courseKindLabel(course.courseKind),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = course.name, style = MaterialTheme.typography.titleMedium)
            if (course.courseKind.showsDosageAndStock && course.dosage.isNotBlank()) {
                Text(
                    text = course.dosage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            course.savedReference?.let { ref ->
                Text(
                    text = stringResource(R.string.course_saved_reference, ref.displayLine()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                text = stringResource(
                    R.string.course_summary,
                    course.durationDays,
                    dateFormatter.format(course.startDate),
                    course.timesOfDay.joinToString { it.toString() },
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun courseKindLabel(kind: CourseKind): String =
    when (kind) {
        CourseKind.MEDICATION -> stringResource(R.string.course_kind_medication)
        CourseKind.SUPPLEMENT -> stringResource(R.string.course_kind_supplement)
        CourseKind.PLANNED_ACTION -> stringResource(R.string.course_kind_planned_action)
    }
