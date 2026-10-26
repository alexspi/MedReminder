package com.example.medreminder.domain

import com.example.medreminder.data.local.DoseStatus
import com.example.medreminder.domain.model.DoseEvent
import com.example.medreminder.domain.model.MedicationCourse
import java.time.LocalDateTime

object DoseScheduleGenerator {
    fun generate(course: MedicationCourse): List<DoseEvent> {
        if (course.durationDays <= 0 || course.timesOfDay.isEmpty()) return emptyList()
        val events = mutableListOf<DoseEvent>()
        for (dayOffset in 0 until course.durationDays) {
            val date = course.startDate.plusDays(dayOffset.toLong())
            for (time in course.timesOfDay.sorted()) {
                events += DoseEvent(
                    courseId = course.id,
                    scheduledAt = LocalDateTime.of(date, time),
                    status = DoseStatus.PENDING,
                )
            }
        }
        return events
    }
}
