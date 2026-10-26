package com.example.medreminder.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medreminder.R
import com.example.medreminder.ui.components.DoseActions
import com.example.medreminder.ui.components.DoseItemCard
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthTitleFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru"))
private val dayTitleFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: kotlinx.coroutines.flow.StateFlow<CalendarUiState>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onMarkTaken: (Long) -> Unit,
    onMarkSkipped: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
) {
    val actions = DoseActions(onMarkTaken, onMarkSkipped, onSnooze)
    val uiState by state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.calendar_title)) })
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthHeader(
                    month = uiState.month,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                )
            }
            item {
                MonthGrid(
                    month = uiState.month,
                    selectedDate = uiState.selectedDate,
                    datesWithDoses = uiState.datesWithDoses,
                    onSelectDate = onSelectDate,
                )
            }
            item {
                Text(
                    text = stringResource(R.string.calendar_day_title, dayTitleFormatter.format(uiState.selectedDate)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (uiState.selectedDayDoses.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.calendar_day_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(uiState.selectedDayDoses, key = { it.dose.id }) { dose ->
                    DoseItemCard(item = dose, showTime = true, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.prev_month))
        }
        Text(
            text = monthTitleFormatter.format(month.atDay(1)).replaceFirstChar { it.titlecase(Locale.forLanguageTag("ru")) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    datesWithDoses: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val weekDays = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day.getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("ru")),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val first = month.atDay(1)
        val offset = (first.dayOfWeek.value - DayOfWeek.MONDAY.value).let { if (it < 0) it + 7 else it }
        val daysInMonth = month.lengthOfMonth()
        val cells = offset + daysInMonth
        val rows = (cells + 6) / 7
        var dayCounter = 1
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cellIndex >= offset && dayCounter <= daysInMonth) {
                            val date = month.atDay(dayCounter)
                            DayCell(
                                day = dayCounter,
                                isSelected = date == selectedDate,
                                hasDoses = datesWithDoses.contains(date),
                                isToday = date == LocalDate.now(),
                                onClick = { onSelectDate(date) },
                            )
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    hasDoses: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        )
        if (hasDoses) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                    ),
            )
        }
    }
}
