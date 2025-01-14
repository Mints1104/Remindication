package com.mints.mobilehealthapplication.data

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import kotlinx.coroutines.tasks.await


object FireStoreRepository {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth = Firebase.auth

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



}
