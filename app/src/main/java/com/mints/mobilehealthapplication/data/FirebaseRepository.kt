package com.mints.mobilehealthapplication.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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

  fun getCurrentUser() = auth.currentUser

}