package com.example.medreminder.update

data class AvailableUpdate(
    val versionCode: Int,
    val versionName: String,
    val apkDownloadUrl: String,
    val releaseNotes: String?,
)

sealed class UpdateCheckResult {
    data object NotConfigured : UpdateCheckResult()

    data object UpToDate : UpdateCheckResult()

    data class Available(val update: AvailableUpdate) : UpdateCheckResult()

    data class Error(val message: String) : UpdateCheckResult()
}
