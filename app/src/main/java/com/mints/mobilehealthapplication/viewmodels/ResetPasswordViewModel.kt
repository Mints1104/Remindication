package com.mints.mobilehealthapplication.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ResetPasswordViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        if (email.isNotEmpty()) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, "Password reset email sent.")
                    } else {
                        val errorMessage = task.exception?.message ?: "Error occurred"
                        onResult(false, errorMessage)
                    }
                }
        } else {
            onResult(false, "Please enter a valid email address.")
        }
    }
}