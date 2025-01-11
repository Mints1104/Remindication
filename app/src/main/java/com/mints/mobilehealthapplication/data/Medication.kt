package com.mints.mobilehealthapplication.data

data class MedicationInfo(
    val name: String,
    val rxcui: String,
    val dosage: String, // Entered by user
    val sideEffects: String, // Fetched from API
    val purpose: String, // Fetched from API
    val time: String // Entered by user
)
