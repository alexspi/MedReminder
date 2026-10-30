package com.example.medreminder.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface GrlsApi {
    @GET("api/grls-records")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("status") status: String = "",
    ): GrlsSearchResponse

    @GET("api/drug-suggestions")
    suspend fun suggestions(
        @Query("q") query: String,
    ): GrlsSuggestionsResponse
}
