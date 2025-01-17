package com.mints.mobilehealthapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ViewModel responsible for managing the registration process including user data and medication information.
 * Handles user authentication, data validation, and storage in Firebase.
 */
class RegistrationViewModel : ViewModel() {
    private val auth = Firebase.auth

    // Encapsulated MutableStateFlows
    private val _registrationData = MutableStateFlow(RegistrationData())
    val registrationData = _registrationData.asStateFlow()

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Initial)
    val registrationState = _registrationState.asStateFlow()

    /**
     * Data class containing all registration-related information.
     * Uses nullable types for optional medication fields.
     */
    data class RegistrationData(
        var email: String = "",
        var password: String = "",
        var firstName: String = "",
        var lastName: String = "",
        var dateOfBirth: String = "",
        var phoneNumber: String = "",
        // Medication fields are optional during registration
        var medicationName: String = "",
        var dosage: String = "",
        var frequency: String = "",
        var reminderTime: String = "",
        var theme: String = "Light",
        var enableNotifications: Boolean = true
    )

    /**
     * Sealed class representing all possible states during the registration process.
     * Provides type-safe state handling.
     */
    sealed class RegistrationState {
        data object Initial : RegistrationState()
        data object Loading : RegistrationState()
        data class Error(val message: String, val type: ErrorType = ErrorType.GENERAL) : RegistrationState()
        data class Success(val userId: String) : RegistrationState()
    }

    /**
     * Enum class representing different types of registration errors.
     * Used for more specific error handling in the UI.
     */
    enum class ErrorType {
        EMAIL_EXISTS,
        WEAK_PASSWORD,
        INVALID_EMAIL,
        AGE_RESTRICTION,
        GENERAL
    }

    /**
     * Validates if the user meets the minimum age requirement (18 years).
     * Uses device's locale for date parsing.
     */
    fun isAgeValid(dobString: String): Boolean {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dob = try {
            dateFormat.parse(dobString)
        } catch (e: ParseException) {
            e.printStackTrace()
            return false
        } ?: return false

        val dobCalendar = Calendar.getInstance().apply { time = dob }
        val currentCalendar = Calendar.getInstance()

        // Calculate age considering month and day
        val age = currentCalendar.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR) -
                if (currentCalendar.get(Calendar.MONTH) < dobCalendar.get(Calendar.MONTH) ||
                    (currentCalendar.get(Calendar.MONTH) == dobCalendar.get(Calendar.MONTH) &&
                            currentCalendar.get(Calendar.DAY_OF_MONTH) < dobCalendar.get(Calendar.DAY_OF_MONTH))) {
                    1
                } else {
                    0
                }

        return age >= 18
    }

    /**
     * Handles the complete registration process including user creation and data storage.
     * Executes in a coroutine scope and handles various Firebase exceptions.
     */
    fun registerUser() = viewModelScope.launch {
        _registrationState.value = RegistrationState.Loading

        try {
            // Validate age before proceeding
            if (!isAgeValid(_registrationData.value.dateOfBirth)) {
                _registrationState.value = RegistrationState.Error(
                    "You must be 18 or older to register",
                    ErrorType.AGE_RESTRICTION
                )
                return@launch
            }

            // Create Firebase user
            val authResult = auth.createUserWithEmailAndPassword(
                _registrationData.value.email,
                _registrationData.value.password
            ).await()

            val user = authResult.user
            if (user != null) {
                // Save user data
                val userData = _registrationData.value
                val isSaved = FireStoreRepository.saveUserData(user.uid, userData)

                if (!isSaved) {
                    _registrationState.value = RegistrationState.Error(
                        "Unable to save your information. Please try again.",
                        ErrorType.GENERAL
                    )
                    return@launch
                }

                // Save medication if provided
                if (userData.medicationName.isNotEmpty()) {
                    val medication = Medication(
                        name = userData.medicationName,
                        dosage = userData.dosage,
                        frequency = userData.frequency,
                        time = userData.reminderTime
                    )

                    val isMedicationSaved = FireStoreRepository.saveMedication(user.uid, medication)
                    if (!isMedicationSaved) {
                        _registrationState.value = RegistrationState.Error(
                            "Your account was created but medication details couldn't be saved. " +
                                    "You can add them later from your profile.",
                            ErrorType.GENERAL
                        )
                        return@launch
                    }
                }

                _registrationState.value = RegistrationState.Success(user.uid)
            } else {
                _registrationState.value = RegistrationState.Error(
                    "Unable to create your account. Please try again.",
                    ErrorType.GENERAL
                )
            }
        } catch (e: Exception) {
            val (message, type) = when (e) {
                is FirebaseAuthUserCollisionException -> Pair(
                    "This email is already registered. Please use a different email or try logging in.",
                    ErrorType.EMAIL_EXISTS
                )
                is FirebaseAuthWeakPasswordException -> Pair(
                    "Please use a stronger password with at least 6 characters.",
                    ErrorType.WEAK_PASSWORD
                )
                is FirebaseAuthInvalidCredentialsException -> Pair(
                    "Please enter a valid email address.",
                    ErrorType.INVALID_EMAIL
                )
                else -> Pair(
                    e.message ?: "Registration failed. Please try again.",
                    ErrorType.GENERAL
                )
            }
            _registrationState.value = RegistrationState.Error(message, type)
        }
    }

    /**
     * Updates registration data in a thread-safe way using the provided update function.
     * @param update lambda function to modify registration data
     */
    fun updateRegistrationData(update: RegistrationData.() -> Unit) {
        _registrationData.value = _registrationData.value.copy().apply(update)
    }
}