package com.mints.mobilehealthapplication.data

import retrofit2.http.GET
import retrofit2.http.Query

interface RxNormApi {
    @GET("REST/approximateTerm.json")
    suspend fun searchMedication(
        @Query("term") name: String,
        @Query("maxEntries") maxEntries: Int = 1
    ): RxNormResponse
}
