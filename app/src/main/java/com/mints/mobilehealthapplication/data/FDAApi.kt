package com.mints.mobilehealthapplication.data

import retrofit2.http.GET
import retrofit2.http.Query

interface FDAApi {
    @GET("drug/label.json")
    suspend fun getMedicationInfo(
        @Query("search") search: String,
        @Query("limit") limit: Int = 1
    ): MedicationResponse
}