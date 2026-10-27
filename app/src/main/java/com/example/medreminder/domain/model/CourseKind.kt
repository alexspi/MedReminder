package com.example.medreminder.domain.model

enum class CourseKind(val storageKey: String) {
    MEDICATION("medication"),
    SUPPLEMENT("supplement"),
    PLANNED_ACTION("planned_action"),
    ;

    val showsDosageAndStock: Boolean get() = this != PLANNED_ACTION

    val showsDrugReference: Boolean get() = this != PLANNED_ACTION

    companion object {
        fun fromStorage(key: String?): CourseKind =
            entries.find { it.storageKey == key } ?: MEDICATION
    }
}
