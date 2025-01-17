package com.mints.mobilehealthapplication.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import kotlinx.coroutines.tasks.await

object FirebaseRepository {
    private val auth: FirebaseAuth = Firebase.auth

    suspend fun sendPasswordResetEmail(email: String): Pair<Boolean, String> {
        return try {
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email).await()
                Pair(true, "Password reset email sent.")
            } else {
                Pair(false, "Please enter a valid email address.")
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Error occurred")
        }
    }

    suspend fun login(email: String, password: String): Pair<Boolean, String> {
        return try {
            if (email.isEmpty() || password.isEmpty()) {
                Pair(false, "Please enter both email and password.")
            } else {
                auth.signInWithEmailAndPassword(email, password).await()
                Pair(true, "Login successful.")
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Login failed.")
        }
    }

    /**
     * Creates a new user account with Firebase Authentication
     * @return Pair<String?, Pair<String, ErrorType>> where first is userId on success, null on failure
     * Second is the error message and type
     */
    suspend fun createUser(email: String, password: String): Pair<String?, Pair<String, RegistrationViewModel.ErrorType>> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Pair(result.user?.uid, Pair("", RegistrationViewModel.ErrorType.GENERAL))
        } catch (e: Exception) {
            val (message, type) = when (e) {
                is FirebaseAuthUserCollisionException -> Pair(
                    "This email is already registered. Please use a different email or try logging in.",
                    RegistrationViewModel.ErrorType.EMAIL_EXISTS
                )
                is FirebaseAuthWeakPasswordException -> Pair(
                    "Please use a stronger password with at least 6 characters.",
                    RegistrationViewModel.ErrorType.WEAK_PASSWORD
                )
                is FirebaseAuthInvalidCredentialsException -> Pair(
                    "Please enter a valid email address.",
                    RegistrationViewModel.ErrorType.INVALID_EMAIL
                )
                else -> Pair(
                    e.message ?: "Registration failed. Please try again.",
                    RegistrationViewModel.ErrorType.GENERAL
                )
            }
            Pair(null, Pair(message, type))
        }
    }

  fun getCurrentUser() = auth.currentUser

}