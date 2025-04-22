// MedicationHistoryViewModel.kt
package com.mints.mobilehealthapplication.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationHistory
import kotlinx.coroutines.launch

class MedicationHistoryViewModel : ViewModel() {

    private val _medications = MutableLiveData<List<Medication>>()
    val medications: LiveData<List<Medication>> get() = _medications

    private val _navigateToDetails = MutableLiveData<Medication?>()
    val navigateToDetails: LiveData<Medication?> get() = _navigateToDetails

    private val _medicationHistories = MutableLiveData<Map<String, MedicationHistory>>()
    val medicationHistories: LiveData<Map<String, MedicationHistory>> get() = _medicationHistories


    private val tag = "MHistoryViewModel"

    private var currentUserId: String? = null


    fun getMedications(uid: String, onComplete: () -> Unit = {}) {
        Log.d(tag, "Starting real-time medication listener for user: $uid")
        FireStoreRepository.getMedicationsSnapshot(uid) { meds, error ->
            if (error != null) {
                Log.e(tag, "Failed to get medications: ${error.message}")
                _medications.postValue(emptyList())
                onComplete()
                return@getMedicationsSnapshot
            }
            Log.d(tag, "Fetched ${meds.size} medications from snapshot")
            _medications.postValue(meds)
            onComplete()
        }
    }


    private fun getMedicationHistory(medicationId: String): MedicationHistory {
        return medicationHistories.value?.get(medicationId) ?: MedicationHistory()
    }

    fun getComplianceRate(): Double {
        val medications = _medications.value ?: return 0.0

        if (medications.isEmpty()) return 0.0

        var totalEvents = 0
        var totalTaken = 0

        medications.forEach { med ->
            val events = med.medicationHistory.getAllEvents()
            totalEvents += events.size
            totalTaken += events.count { it.type == MedicationEvent.EventType.TAKEN }
        }

        if (totalEvents == 0) return 0.0

        return (totalTaken.toDouble() / totalEvents) * 100
    }



    fun testReceivingMedicationHistory(medication: Medication) {
        viewModelScope.launch {
            val history = medication.id?.let { getMedicationHistory(it) }
            if(history == null) return@launch
            Log.d(tag, "History: $history")
            history.getLastEventOfType(MedicationEvent.EventType.TAKEN)?.let { lastTaken ->
                Log.d(tag, "Last taken: ${lastTaken.date}")
            }
            val compliance = history.getComplianceRate()
            Log.d(tag, "Compliance rate: $compliance%")
            if (history.wasTakenToday()) {
                    Log.d(tag, "Medication already taken today")
                }
            val recentEvents = history.getEventsFromLastDays(7)
            Log.d(tag, "Events in last 7 days: ${recentEvents.size}")
        }
    }

    fun onMedicationClicked(medication: Medication) {
        _navigateToDetails.value = medication
    }

    // Clear navigation event once handled
    fun onMedicationNavigated() {
        _navigateToDetails.value = null
    }

    fun getMedicationList(): MutableLiveData<List<Medication>> = _medications
}
