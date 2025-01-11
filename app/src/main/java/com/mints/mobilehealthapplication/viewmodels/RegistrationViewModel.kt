package com.mints.mobilehealthapplication.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RegistrationViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    // Expose RegistrationData as a MutableStateFlow
    private val _registrationData = MutableStateFlow(RegistrationData())
    val registrationData = _registrationData.asStateFlow()

    // State flow for registration state
    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Initial)
    val registrationState = _registrationState.asStateFlow()

    // Data class to hold registration data
    data class RegistrationData(
        var email: String = "",
        var password: String = "",
        var firstName: String = "",
        var lastName: String = "",
        var dateOfBirth: String = "",
        var phoneNumber: String = "",
        var medicationName: String = "",
        var dosage: String = "",
        var frequency: String = "",
        var reminderTime: String = "",
        var theme: String = "Light", // Default theme
        var enableNotifications: Boolean = true // Default notification preference
    )

    // Sealed class to represent registration states
    sealed class RegistrationState {
        data object Initial : RegistrationState()
        data object Loading : RegistrationState()
        data class Error(val message: String) : RegistrationState()
        data object Success : RegistrationState()
    }

    // Function to register a new user
    fun registerUser() {
        _registrationState.value = RegistrationState.Loading

        val data = _registrationData.value

        // Create user with Firebase Authentication
        auth.createUserWithEmailAndPassword(data.email, data.password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    // Save additional user data to Firestore
                    saveUserData(user.uid, data)
                } else {
                    _registrationState.value = RegistrationState.Error("User creation failed")
                }
            }
            .addOnFailureListener {
                _registrationState.value = RegistrationState.Error(it.message ?: "Registration failed")
            }
    }

    // Function to save user data to Firestore
    private fun saveUserData(uid: String, data: RegistrationData) {
        val userData = hashMapOf(
            "uid" to uid,
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

        // Save data to Firestore
        db.collection("users").document(uid)
            .set(userData)
            .addOnSuccessListener {
                _registrationState.value = RegistrationState.Success
            }
            .addOnFailureListener {
                _registrationState.value = RegistrationState.Error(it.message ?: "Failed to save user data")
            }
    }

    // Function to update registration data
    fun updateRegistrationData(update: RegistrationData.() -> Unit) {
        _registrationData.value = _registrationData.value.apply(update)
    }
}