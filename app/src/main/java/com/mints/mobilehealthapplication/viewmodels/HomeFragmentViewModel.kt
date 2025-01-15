package com.mints.mobilehealthapplication.viewmodels

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

    // Function to load medications for a user
    fun getMedications(uid: String) {
        viewModelScope.launch {
            val meds = FireStoreRepository.getMedications(uid)
            if (meds != null) {
                _medications.postValue(meds!!)
            } else {
                // Handle error (you can post an error state or a message)
            }
        }
    }
}
