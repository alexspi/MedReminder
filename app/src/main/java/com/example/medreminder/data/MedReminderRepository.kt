package com.example.medreminder.data

import com.example.medreminder.data.local.AppDatabase
import com.example.medreminder.data.local.DoseStatus
import com.example.medreminder.data.local.entity.DoseEventEntity
import com.example.medreminder.data.local.entity.MedicationCourseEntity
import com.example.medreminder.domain.DoseScheduleGenerator
import com.example.medreminder.domain.model.DoseEvent
import com.example.medreminder.domain.model.DoseWithCourse
import com.example.medreminder.domain.model.MedicationCourse
import com.example.medreminder.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MedReminderRepository(
    database: AppDatabase,
    private val scheduler: ReminderScheduler,
) {
    private val courseDao = database.medicationCourseDao()
    private val doseDao = database.doseEventDao()
    private val zone = ZoneId.systemDefault()

    fun observeCourses(): Flow<List<MedicationCourse>> =
        courseDao.observeAll().map { list -> list.map(MedicationCourse::fromEntity) }

    suspend fun getCourse(id: Long): MedicationCourse? =
        courseDao.getById(id)?.let(MedicationCourse::fromEntity)

    fun observeTodayDoses(date: LocalDate = LocalDate.now()): Flow<List<DoseWithCourse>> =
        observeDosesForDate(date)

    fun observeDosesForDate(date: LocalDate): Flow<List<DoseWithCourse>> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return combine(
            doseDao.observeForDay(start, end),
            courseDao.observeAll(),
        ) { doses, courses -> mapDosesWithCourses(doses, courses) }
    }

    fun observeDosesInRange(start: LocalDate, endExclusive: LocalDate): Flow<List<DoseWithCourse>> {
        val rangeStart = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEnd = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
        return combine(
            doseDao.observeInRange(rangeStart, rangeEnd),
            courseDao.observeAll(),
        ) { doses, courses -> mapDosesWithCourses(doses, courses) }
    }

    fun observeWeekHistory(): Flow<List<DoseWithCourse>> {
        val today = LocalDate.now()
        val rangeStart = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return combine(
            doseDao.observeTakenOrSkippedInRange(rangeStart, rangeEnd),
            courseDao.observeAll(),
        ) { doses, courses -> mapDosesWithCourses(doses, courses) }
    }

    private fun mapDosesWithCourses(
        doses: List<DoseEventEntity>,
        courses: List<MedicationCourseEntity>,
    ): List<DoseWithCourse> {
        val courseById = courses.associate { it.id to MedicationCourse.fromEntity(it) }
        return doses.mapNotNull { entity ->
            val course = courseById[entity.courseId] ?: return@mapNotNull null
            DoseWithCourse(DoseEvent.fromEntity(entity), course)
        }
    }

    suspend fun getDoseWithCourse(doseId: Long): DoseWithCourse? {
        val entity = doseDao.getById(doseId) ?: return null
        val course = courseDao.getById(entity.courseId)?.let(MedicationCourse::fromEntity) ?: return null
        return DoseWithCourse(DoseEvent.fromEntity(entity), course)
    }

    suspend fun getPendingDosesForScheduling(): List<DoseEvent> =
        doseDao.getByStatusAfter(System.currentTimeMillis(), DoseStatus.PENDING)
            .map(DoseEvent::fromEntity)

    suspend fun saveCourse(course: MedicationCourse): Long {
        val entity = course.toEntity()
        val courseId = if (entity.id == 0L) {
            courseDao.insert(entity)
        } else {
            courseDao.update(entity)
            entity.id
        }
        val saved = course.copy(id = courseId)
        doseDao.deleteByCourseId(courseId)
        val events = DoseScheduleGenerator.generate(saved).map { it.toEntity() }
        if (events.isNotEmpty()) {
            doseDao.insertAll(events)
        }
        scheduler.rescheduleAll(this)
        return courseId
    }

    suspend fun deleteCourse(id: Long) {
        courseDao.deleteById(id)
        scheduler.rescheduleAll(this)
    }

    suspend fun markTaken(doseId: Long) {
        updateStatus(doseId, DoseStatus.TAKEN, takenAt = LocalDateTime.now())
        scheduler.cancel(doseId)
    }

    suspend fun markSkipped(doseId: Long) {
        updateStatus(doseId, DoseStatus.SKIPPED, takenAt = null)
        scheduler.cancel(doseId)
    }

    suspend fun snooze(doseId: Long, minutes: Long = SNOOZE_MINUTES) {
        val entity = doseDao.getById(doseId) ?: return
        val newAt = LocalDateTime.now().plusMinutes(minutes)
        val updated = entity.copy(
            scheduledAtEpochMillis = newAt.atZone(zone).toInstant().toEpochMilli(),
            status = DoseStatus.PENDING,
            takenAtEpochMillis = null,
        )
        doseDao.update(updated)
        scheduler.cancel(doseId)
        scheduler.schedule(DoseEvent.fromEntity(updated))
    }

    private suspend fun updateStatus(doseId: Long, status: DoseStatus, takenAt: LocalDateTime?) {
        val entity = doseDao.getById(doseId) ?: return
        val takenMillis = takenAt?.atZone(zone)?.toInstant()?.toEpochMilli()
        doseDao.update(
            entity.copy(
                status = status,
                takenAtEpochMillis = takenMillis,
            ),
        )
    }

    companion object {
        const val SNOOZE_MINUTES = 10L
    }
}
