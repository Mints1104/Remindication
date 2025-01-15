package com.mints.mobilehealthapplication.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import kotlinx.coroutines.launch

class MedicationViewModel : ViewModel() {

    val saveResult = MutableLiveData<Boolean>()

    fun saveMedication(uid: String, medication: Medication) {
        viewModelScope.launch {
            val success = FireStoreRepository.saveMedication(uid, medication)
            saveResult.postValue(success)
        }
    }
}