package com.example.medreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_courses")
data class MedicationCourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val startDateEpochDay: Long,
    val durationDays: Int,
    /** Times of day as "HH:mm" separated by commas, sorted ascending. */
    val timesOfDayCsv: String,
    val notes: String,
    val soundUri: String?,
    val isActive: Boolean = true,
    val referenceOptionId: String? = null,
    val referenceTradeName: String? = null,
    val referenceInnName: String? = null,
    val referenceRegNumber: String? = null,
    val referenceStatus: String? = null,
    val referenceFormsSummary: String? = null,
    val referenceSourceLabel: String? = null,
)
