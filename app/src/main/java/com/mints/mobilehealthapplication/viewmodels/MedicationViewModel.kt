package com.mints.mobilehealthapplication.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import kotlinx.coroutines.launch

class MedicationViewModel : ViewModel() {

    val saveResult = MutableLiveData<Boolean>()

    fun saveMedication(uid: String, medication: Medication) {
        Log.d("MedicationViewModel", "Saving medication for user: $uid")
        Log.d("MedicationViewModel", "Medication details: $medication")

        viewModelScope.launch {
            val success = FireStoreRepository.saveMedication(uid, medication)
            Log.d("MedicationViewModel", "Medication save result: $success")
            saveResult.postValue(success)
        }
    }
}