package com.example.medreminder.domain.model

import com.example.medreminder.data.local.DoseStatus
import com.example.medreminder.data.local.entity.DoseEventEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class DoseEvent(
    val id: Long = 0,
    val courseId: Long,
    val scheduledAt: LocalDateTime,
    val status: DoseStatus,
    val takenAt: LocalDateTime? = null,
) {
    fun toEntity(): DoseEventEntity =
        DoseEventEntity(
            id = id,
            courseId = courseId,
            scheduledAtEpochMillis = scheduledAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            status = status,
            takenAtEpochMillis = takenAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        )

    companion object {
        fun fromEntity(entity: DoseEventEntity): DoseEvent {
            val zone = ZoneId.systemDefault()
            return DoseEvent(
                id = entity.id,
                courseId = entity.courseId,
                scheduledAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(entity.scheduledAtEpochMillis),
                    zone,
                ),
                status = entity.status,
                takenAt = entity.takenAtEpochMillis?.let {
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zone)
                },
            )
        }
    }
}
