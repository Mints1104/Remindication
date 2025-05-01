package com.mints.mobilehealthapplication.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent

class MedicationAnalyticsViewModel: ViewModel() {

    private val _medications = MutableLiveData<List<Medication>>()
    val medications: LiveData<List<Medication>> get() = _medications

    private val _adherencePercentage = MutableLiveData<Float>()
    val adherencePercentage: LiveData<Float> get() = _adherencePercentage

    private val _medicationEvents = MutableLiveData<List<MedicationEvent>>()
    val medicationEvents: LiveData<List<MedicationEvent>> get() = _medicationEvents

    private val tag = "MedicationAnalyticsVM"

    fun getMedications(uid: String, onComplete: () -> Unit = {}) {
        Log.d(tag, "Starting real-time medication listener for user: $uid")

        FireStoreRepository.getMedicationsSnapshot(uid) { meds, error ->
            if (error != null) {
                Log.e(tag, "Failed to get medications: ${error.message}")
                onComplete()
                return@getMedicationsSnapshot
            }

            Log.d(tag, "Fetched ${meds.size} medications from snapshot")
            _medications.postValue(meds)
            calculateOverallMedicationAdherence(meds)
            onComplete()
        }
    }

    private fun calculateOverallMedicationAdherence(medications: List<Medication>) {
        Log.d(tag, "Calculating overall medication adherence")
        if (medications.isEmpty()) {
            Log.d(tag, "No medications to calculate adherence for")
            _adherencePercentage.postValue(0f)
            _medicationEvents.postValue(emptyList())
            return
        }

        Log.d(tag, "Collecting events for ${medications.size} medications")

        val allEvents = mutableListOf<MedicationEvent>()
        medications.forEach { medication ->
            allEvents.addAll(medication.medicationHistory.getAllEvents())
        }

        _medicationEvents.postValue(allEvents)

        Log.d(tag, "Collected ${allEvents.size} medication events for visualization")
    }
}