package com.example.medreminder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.medreminder.data.local.DoseStatus

@Entity(
    tableName = "dose_events",
    foreignKeys = [
        ForeignKey(
            entity = MedicationCourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("courseId"), Index("scheduledAtEpochMillis")],
)
data class DoseEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val scheduledAtEpochMillis: Long,
    val status: DoseStatus,
    val takenAtEpochMillis: Long? = null,
)
