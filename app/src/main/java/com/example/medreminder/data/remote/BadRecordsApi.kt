package com.example.medreminder.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/** Поиск по реестру зарегистрированных БАД (данные pharmcontrol.ru). */
interface BadRecordsApi {
    @GET("api/bad-records")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
    ): GrlsSearchResponse
}
