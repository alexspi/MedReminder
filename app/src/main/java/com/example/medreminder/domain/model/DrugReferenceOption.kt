package com.example.medreminder.domain.model

data class DrugReferenceOption(
    val id: String,
    val tradeName: String,
    val innName: String,
    val status: String,
    val registrationNumber: String?,
    val dosageFormsSummary: String,
    val pharmGroup: String?,
    val sourceLabel: String,
)

data class SavedDrugReference(
    val optionId: String,
    val tradeName: String,
    val innName: String,
    val registrationNumber: String?,
    val status: String?,
    val dosageFormsSummary: String?,
    val sourceLabel: String,
) {
    fun displayLine(): String = buildString {
        append(tradeName)
        if (innName.isNotBlank() && innName != "—") {
            append(" · ")
            append(innName)
        }
        if (!registrationNumber.isNullOrBlank()) {
            append(" · ")
            append(registrationNumber)
        }
    }
}
