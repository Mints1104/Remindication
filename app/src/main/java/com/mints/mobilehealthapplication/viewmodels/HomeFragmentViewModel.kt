package com.mints.mobilehealthapplication.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationSchedule
import kotlinx.coroutines.launch
import java.time.LocalDateTime

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

    fun deleteMedication(uid: String, medicationId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                FireStoreRepository.deleteMedication(uid, medicationId)
                onComplete()
            } catch (e: Exception) {
                Log.e("HomeFragmentVM", "Delete failed: ${e.message}")
                // Consider showing error to user
            }
        }
    }

    private fun debugAdvanceDates(medication: Medication): Medication {
        return when (val schedule = medication.schedule) {
            is MedicationSchedule.Daily -> {
                val newDates = schedule.nextDueDates.map { it.plusDays(1) }
                Log.d("DEBUG_DATES", "OLD DATES: ${schedule.nextDueDates}")
                Log.d("DEBUG_DATES", "NEW DATES: $newDates")

                // Create a new Daily schedule with updated dates
                medication.copy(
                    schedule = schedule.copy(nextDueDates = newDates)
                )
            }
            // Handle other schedule types if needed
            else -> {
                Log.d("DEBUG_DATES", "Non-daily schedule - no change")
                medication // Return unchanged
            }
        }
    }

    fun markMedicationAsTaken(userId: String, medication: Medication) {
        val updatedMedication = debugAdvanceDates(medication)

        viewModelScope.launch {
            updatedMedication.id?.let { medId ->
                if (updatedMedication.schedule is MedicationSchedule.Daily) {
                    val success = FireStoreRepository.updateMedicationDates(
                        userId = userId,
                        medicationId = medId,
                        newDates = updatedMedication.schedule.nextDueDates
                    )

                    if (success) {
                        // Update local list
                        _medications.value = _medications.value?.map {
                            if (it.id == medication.id) updatedMedication else it
                        }
                    } else {

                    }
                }
            }
        }
    }



        fun testFirestoreUpdate() {
            viewModelScope.launch {
                val testDates = listOf(LocalDateTime.now().plusDays(1))
                val success = FireStoreRepository.updateMedicationDates(
                    userId = "test_user_id",
                    medicationId = "test_med_id",
                    newDates = testDates
                )
                Log.d("FIREBASE_TEST", "Update success: $success")
            }
        }


        fun getMedicationList(): MutableLiveData<List<Medication>> = _medications


        fun onMedicationClicked(medication: Medication) {
            _navigateToDetails.value = medication // Trigger navigation
        }

        fun onNavigationComplete() {
            _navigateToDetails.value = null // Reset navigation event
        }


    }
