package com.mints.mobilehealthapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mints.mobilehealthapplication.data.FirebaseRepository
import kotlinx.coroutines.launch

class ResetPasswordViewModel : ViewModel() {


    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = FirebaseRepository.sendPasswordResetEmail(email)
                onResult(result.first, result.second)
            } catch (e: Exception) {
                onResult(false, e.message ?: "An error occurred")
            }
        }
    }


}