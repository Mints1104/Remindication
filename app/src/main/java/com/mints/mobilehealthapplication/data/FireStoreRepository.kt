package com.mints.mobilehealthapplication.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import kotlinx.coroutines.tasks.await


object FireStoreRepository {
    private val db by lazy { FirebaseFirestore.getInstance() }

    /**
     * Saves user data to Firestore.
     * @param uid The user ID.
     * @param data The registration data to save.
     */
    suspend fun saveUserData(uid: String, data: RegistrationViewModel.RegistrationData): Boolean {
        return try {
            val userData = hashMapOf(
                "email" to data.email,
                "firstName" to data.firstName,
                "lastName" to data.lastName,
                "dateOfBirth" to data.dateOfBirth,
                "phoneNumber" to data.phoneNumber,
                "medication" to hashMapOf(
                    "name" to data.medicationName,
                    "dosage" to data.dosage,
                    "frequency" to data.frequency,
                    "reminderTime" to data.reminderTime
                ),
                "preferences" to hashMapOf(
                    "theme" to data.theme,
                    "enableNotifications" to data.enableNotifications
                ),
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("users").document(uid).set(userData).await()
            true // Success
        } catch (e: Exception) {
            false // Failure
        }
    }

    suspend fun saveMedication(uid: String, medication: Medication): Boolean {
        return try {
            val medicationData = hashMapOf(
                "name" to medication.name,
                "dosage" to medication.dosage,
                "frequency" to medication.frequency,
                "time" to medication.time,
                "notes" to medication.notes,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("medications")
                .add(medicationData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getMedications(uid: String): List<Medication>? {
        return try {
            // Initialize the list to store medications
            val medicationList = mutableListOf<Medication>()

            // Get medications from Firestore
            val snapshot = db.collection("users").document(uid).collection("medications").get().await()

            // Loop through documents and map them to Medication objects
            for (document in snapshot.documents) {
                val medication = document.getTimestamp("createdAt")?.let {
                    Medication(
                        name = document.getString("name") ?: "",
                        dosage = document.getString("dosage") ?: "",
                        frequency = document.getString("frequency") ?: "",
                        time = document.getString("time") ?: "",
                        notes = document.getString("notes") ?: "",
                        createdAt = it
                    )
                }
                if (medication != null) {
                    medicationList.add(medication)
                }
            }

            medicationList // Return the list of medications

        } catch (e: Exception) {
            e.printStackTrace()  // Log error for debugging
            null  // Return null in case of error
        }
    }




}
