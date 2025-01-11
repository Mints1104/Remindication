package com.mints.mobilehealthapplication.data

import android.util.Log

class MedicationRepository {
    suspend fun searchMedication(name: String): Result<String> {
        return try {
            Log.d("API_RESPONSE", "Searching for medication: $name")
            val response = RetrofitClient.rxNormApi.searchMedication(name)
            Log.d("API_RESPONSE", "Raw response: $response")

            val candidates = response.approximateGroup.candidates
            if (candidates.isNotEmpty()) {
                // Find first candidate with non-null name
                val bestMatch = candidates.firstOrNull { !it.name.isNullOrBlank() }
                    ?: return Result.failure(Exception("No valid medication name found"))

                Log.d("API_RESPONSE", "Best match - Name: ${bestMatch.name}, ID: ${bestMatch.rxcui}, Score: ${bestMatch.score}")
                Result.success("Found medication: ${bestMatch.name} (ID: ${bestMatch.rxcui})")
            } else {
                Log.d("API_RESPONSE", "No candidates found")
                Result.failure(Exception("No medication found"))
            }
        } catch (e: Exception) {
            Log.e("API_RESPONSE", "Error: ${e.message}", e)
            Result.failure(e)
        }
    }
}