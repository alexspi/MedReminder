package com.example.medreminder.domain.model

data class DoseWithCourse(
    val dose: DoseEvent,
    val course: MedicationCourse,
)
