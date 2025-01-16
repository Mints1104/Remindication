package com.mints.mobilehealthapplication.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
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
                val documentReference = db.collection("users")
                    .document(uid)
                    .collection("medications")
                    .add(medicationData)
                    .await()
                medication.id = documentReference.id
                true
            } catch (e: Exception) {
                false
            }
        }

    suspend fun getMedications(uid: String): List<Medication> {
        return db.collection("users")
            .document(uid)
            .collection("medications")
            .get()
            .await()
            .map { document ->
                Medication(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    dosage = document.getString("dosage") ?: "",
                    frequency = document.getString("frequency") ?: "",
                    time = document.getString("time") ?: "",
                    notes = document.getString("notes") ?: "",
                    createdAt = document.getTimestamp("createdAt") ?: Timestamp.now()
                )
            }
    }


    fun retrieveUserInfo(userId: String, onSuccess: (DocumentSnapshot) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }




}
