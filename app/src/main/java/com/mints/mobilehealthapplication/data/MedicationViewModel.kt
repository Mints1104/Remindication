package com.mints.mobilehealthapplication.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = MedicationRepository()
    private val _searchResult = MutableLiveData<Result<String>>()
    val searchResult: LiveData<Result<String>> = _searchResult

    fun searchMedication(name: String) {
        viewModelScope.launch {
            try {
                val result = repository.searchMedication(name)
                _searchResult.value = result
            } catch (e: Exception) {
                _searchResult.value = Result.failure(e)
            }
        }
    }
}