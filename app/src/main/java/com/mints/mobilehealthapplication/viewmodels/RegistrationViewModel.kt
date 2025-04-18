package com.mints.mobilehealthapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.data.FireStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ViewModel responsible for managing the registration process including user data and medication information.
 * Handles user authentication, data validation, and storage in Firebase.
 */
class RegistrationViewModel : ViewModel() {
    private val auth = Firebase.auth

    private val _registrationData = MutableStateFlow(RegistrationData())
    val registrationData = _registrationData.asStateFlow()

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Initial)
    val registrationState = _registrationState.asStateFlow()


    fun resetRegistrationData() {
        _registrationData.value = RegistrationData()
        _registrationState.value = RegistrationState.Initial
    }
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
    fun isAgeValid(dateString: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dob = dateFormat.parse(dateString)
            val dobCalendar = Calendar.getInstance().apply {
                if (dob != null) {
                    time = dob
                }
            }
            val today = Calendar.getInstance()

            val age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR)

            if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
                age - 1 >= 18
            } else {
                age >= 18
            }
        } catch (e: Exception) {
            false // Return false for any parsing errors
        }
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