package com.example.medreminder.domain

import com.example.medreminder.domain.model.MedicationCourse

object StockSupplyCalculator {
    const val LOW_SUPPLY_WARNING_DAYS = 3

    fun tracksInventory(course: MedicationCourse): Boolean =
        course.courseKind.showsDosageAndStock &&
            course.isActive &&
            course.unitsInPackage > 0 &&
            course.unitsPerDose > 0

    fun dailyConsumption(course: MedicationCourse): Int {
        if (course.timesOfDay.isEmpty()) return 0
        return course.unitsPerDose * course.timesOfDay.size
    }

    fun daysOfSupplyRemaining(course: MedicationCourse): Int? {
        if (!tracksInventory(course)) return null
        val daily = dailyConsumption(course)
        if (daily <= 0) return null
        return course.remainingUnits / daily
    }

    fun isLowSupply(course: MedicationCourse): Boolean {
        val days = daysOfSupplyRemaining(course) ?: return false
        return course.remainingUnits > 0 && days <= LOW_SUPPLY_WARNING_DAYS
    }

    fun isDepleted(course: MedicationCourse): Boolean =
        tracksInventory(course) && course.remainingUnits <= 0
}
