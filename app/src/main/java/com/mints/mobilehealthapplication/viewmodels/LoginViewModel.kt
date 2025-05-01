package com.mints.mobilehealthapplication.viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mints.mobilehealthapplication.data.FirebaseRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {


    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = FirebaseRepository.login(email, password)
            onResult(result.first, result.second)
        }
    }
}