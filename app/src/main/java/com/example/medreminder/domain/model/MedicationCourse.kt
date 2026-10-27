package com.example.medreminder.domain.model

import com.example.medreminder.data.local.CourseTimeCodec
import com.example.medreminder.data.local.entity.MedicationCourseEntity
import java.time.LocalDate
import java.time.LocalTime

data class MedicationCourse(
    val id: Long = 0,
    val courseKind: CourseKind = CourseKind.MEDICATION,
    val name: String,
    val dosage: String,
    val startDate: LocalDate,
    val durationDays: Int,
    val timesOfDay: List<LocalTime>,
    val notes: String,
    val soundUri: String?,
    val isActive: Boolean = true,
    val savedReference: SavedDrugReference? = null,
    val unitsInPackage: Int = 0,
    val unitsPerDose: Int = 1,
    val remainingUnits: Int = 0,
) {
    fun toEntity(): MedicationCourseEntity =
        MedicationCourseEntity(
            id = id,
            name = name.trim(),
            dosage = dosage.trim(),
            startDateEpochDay = startDate.toEpochDay(),
            durationDays = durationDays,
            timesOfDayCsv = CourseTimeCodec.encode(timesOfDay),
            notes = notes.trim(),
            soundUri = soundUri,
            isActive = isActive,
            referenceOptionId = savedReference?.optionId,
            referenceTradeName = savedReference?.tradeName,
            referenceInnName = savedReference?.innName,
            referenceRegNumber = savedReference?.registrationNumber,
            referenceStatus = savedReference?.status,
            referenceFormsSummary = savedReference?.dosageFormsSummary,
            referenceSourceLabel = savedReference?.sourceLabel,
            unitsInPackage = unitsInPackage,
            unitsPerDose = unitsPerDose,
            remainingUnits = remainingUnits,
            courseKind = courseKind.storageKey,
        )

    companion object {
        fun fromEntity(entity: MedicationCourseEntity): MedicationCourse {
            val saved = entity.referenceOptionId?.let { id ->
                SavedDrugReference(
                    optionId = id,
                    tradeName = entity.referenceTradeName.orEmpty(),
                    innName = entity.referenceInnName.orEmpty(),
                    registrationNumber = entity.referenceRegNumber,
                    status = entity.referenceStatus,
                    dosageFormsSummary = entity.referenceFormsSummary,
                    sourceLabel = entity.referenceSourceLabel.orEmpty(),
                )
            }
            return MedicationCourse(
                id = entity.id,
                courseKind = CourseKind.fromStorage(entity.courseKind),
                name = entity.name,
                dosage = entity.dosage,
                startDate = LocalDate.ofEpochDay(entity.startDateEpochDay),
                durationDays = entity.durationDays,
                timesOfDay = CourseTimeCodec.decode(entity.timesOfDayCsv),
                notes = entity.notes,
                soundUri = entity.soundUri,
                isActive = entity.isActive,
                savedReference = saved,
                unitsInPackage = entity.unitsInPackage,
                unitsPerDose = entity.unitsPerDose,
                remainingUnits = entity.remainingUnits,
            )
        }
    }
}

fun DrugReferenceOption.toSaved(): SavedDrugReference =
    SavedDrugReference(
        optionId = id,
        tradeName = tradeName,
        innName = innName,
        registrationNumber = registrationNumber,
        status = status,
        dosageFormsSummary = dosageFormsSummary,
        sourceLabel = sourceLabel,
    )
