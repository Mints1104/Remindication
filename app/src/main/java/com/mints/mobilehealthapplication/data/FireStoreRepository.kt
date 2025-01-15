package com.mints.mobilehealthapplication.data

import android.util.Log
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
            Log.d("FireStoreRepository", "Saving medication for user: $uid")
            Log.d("FireStoreRepository", "Medication details: $medication")

            // Create a hashmap for the medication data
            val medicationData = hashMapOf(
                "name" to medication.name,
                "dosage" to medication.dosage,
                "frequency" to medication.frequency,
                "time" to medication.time,
                "notes" to medication.notes,
                "createdAt" to FieldValue.serverTimestamp()
            )

            Log.d("FireStoreRepository", "Medication data to save: $medicationData")

            // Save the medication data to Firestore
            val documentReference = db.collection("users")
                .document(uid)
                .collection("medications")
                .add(medicationData)
                .await()

            Log.d("FireStoreRepository", "Medication saved successfully with ID: ${documentReference.id}")
            true
        } catch (e: Exception) {
            Log.e("FireStoreRepository", "Error saving medication: ${e.message}", e)
            false
        }

    }

    suspend fun getMedications(uid: String): List<Medication> {
        return try {
            Log.d("FireStoreRepository", "Fetching medications for user: $uid")

            // Perform the Firestore query
            val snapshot = db.collection("users")
                .document(uid)
                .collection("medications")
                .get()
                .await()

            Log.d("FireStoreRepository", "Query path: users/$uid/medications")
            Log.d("FireStoreRepository", "Number of documents fetched: ${snapshot.documents.size}")

            // Log each document's data
            snapshot.documents.forEach { document ->
                Log.d("FireStoreRepository", "Document ID: ${document.id}, Data: ${document.data}")
            }

            // Map documents to Medication objects
            val medications = snapshot.documents.mapNotNull { document ->
                val medication = document.toObject(Medication::class.java)
                Log.d("FireStoreRepository", "Mapped medication: ${medication?.name}")
                medication
            }

            Log.d("FireStoreRepository", "Successfully mapped ${medications.size} medications")
            medications
        } catch (e: Exception) {
            Log.e("FireStoreRepository", "Error fetching medications: ${e.message}", e)
            emptyList()
        }
    }




}
