package com.mints.mobilehealthapplication.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import kotlinx.coroutines.launch

class HomeFragmentViewModel : ViewModel() {

    private val _medications = MutableLiveData<List<Medication>>()
    val medications: LiveData<List<Medication>> get() = _medications
    private val _navigateToDetails = MutableLiveData<Medication?>()
    val navigateToDetails: LiveData<Medication?> get() = _navigateToDetails
    fun getMedications(uid: String) {
        Log.d("HomeFragmentViewModel", "Fetching medications for user: $uid")
        viewModelScope.launch {
            val meds = FireStoreRepository.getMedications(uid)
            Log.d("HomeFragmentViewModel", "Fetched ${meds.size} medications")
            _medications.postValue(meds)
        }
    }

    fun onMedicationClicked(medication: Medication) {
        _navigateToDetails.value = medication // Trigger navigation
    }

    fun onNavigationComplete() {
        _navigateToDetails.value = null // Reset navigation event
    }


}