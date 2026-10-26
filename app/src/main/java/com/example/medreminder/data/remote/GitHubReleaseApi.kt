package com.example.medreminder.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubReleaseApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): GitHubReleaseDto
}
