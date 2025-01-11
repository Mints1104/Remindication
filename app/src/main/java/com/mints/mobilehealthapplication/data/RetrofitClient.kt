package com.mints.mobilehealthapplication.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://rxnav.nlm.nih.gov/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val rxNormApi: RxNormApi by lazy {
        retrofit.create(RxNormApi::class.java)
    }
}