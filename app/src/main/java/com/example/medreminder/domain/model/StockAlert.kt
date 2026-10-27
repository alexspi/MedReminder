package com.example.medreminder.domain.model

data class StockAlert(
    val courseId: Long,
    val courseName: String,
    val remainingUnits: Int,
    val daysOfSupplyLeft: Int,
    val depleted: Boolean,
)
