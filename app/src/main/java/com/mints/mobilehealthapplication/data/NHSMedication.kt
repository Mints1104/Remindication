package com.mints.mobilehealthapplication.data
import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class NHSMedication {
// --- Data Classes for Parsing the JSON ---

    data class MedicineResponse(
        val about: About,
        val modules: List<Module>
    )

    data class About(
        val name: String,
        val url: String,
        val webpage: String
    )

    data class Module(
        @SerializedName("@type") val type: String,
        val url: String,
        val hasHealthAspect: String,
        val headline: String,
        val description: String,
        val hasPart: List<WebPart>?
    )

    data class WebPart(
        @SerializedName("@type") val type: String,
        val headline: String,
        val text: String
    )

// --- Retrofit Interface ---

    interface NhsMedicineApi {
        @GET("nhs-website-content/medicines/{medication}/")
        suspend fun getMedicineDetails(
            @Path("medication") medication: String,
            @Query("modules") modules: Boolean = true
        ): Response<MedicineResponse>
    }

// --- Test Function ---

    fun testGetMedicineDetails(medicationName: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://sandbox.api.service.nhs.uk/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(NhsMedicineApi::class.java)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = service.getMedicineDetails(medicationName)
                if (response.isSuccessful) {
                    val medicineResponse = response.body()
                    val description = medicineResponse?.modules?.firstOrNull()?.description
                    Log.d("TestMedicine", "Medication: $medicationName")
                    Log.d("TestMedicine", "Description: $description")
                } else {
                    Log.e("TestMedicine", "Failed for $medicationName: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("TestMedicine", "Exception for $medicationName: ${e.message}", e)
            }
        }
    }

}