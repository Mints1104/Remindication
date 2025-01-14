package com.mints.mobilehealthapplication.viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mints.mobilehealthapplication.data.FirebaseRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    /**
     * Attempts to log in a user with the provided email and password.
     * @param email The user's email.
     * @param password The user's password.
     * @param onResult A callback function that receives a Pair<Boolean, String>:
     *                 - Boolean: Indicates success (true) or failure (false).
     *                 - String: Contains a success message or error message.
     */
    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = FirebaseRepository.login(email, password)
            onResult(result.first, result.second)
        }
    }
}