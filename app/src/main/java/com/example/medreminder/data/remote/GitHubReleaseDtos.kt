package com.example.medreminder.data.remote

import com.google.gson.annotations.SerializedName

data class GitHubReleaseDto(
    @SerializedName("tag_name") val tagName: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("assets") val assets: List<GitHubReleaseAssetDto>?,
)

data class GitHubReleaseAssetDto(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
)

data class ReleaseVersionManifest(
    @SerializedName("versionCode") val versionCode: Int,
    @SerializedName("versionName") val versionName: String,
    @SerializedName("apkFileName") val apkFileName: String? = null,
)
