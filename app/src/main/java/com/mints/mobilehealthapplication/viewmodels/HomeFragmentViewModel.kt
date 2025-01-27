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
import java.time.LocalDate
import java.time.LocalDateTime

class HomeFragmentViewModel : ViewModel() {

     val _medications = MutableLiveData<List<Medication>>()
    val medications: LiveData<List<Medication>> get() = _medications
    private val _navigateToDetails = MutableLiveData<Medication?>()
    val navigateToDetails: LiveData<Medication?> get() = _navigateToDetails

    private var _lastOriginalMedication: Medication? = null
    private var _lastOriginalDates: List<LocalDateTime>? = null

    val lastOriginalDates: List<LocalDateTime>? get() = _lastOriginalDates

    /*
    1. Store the medication we are marking as taken and its original and new date
    2. Upon completion, then we want to temporarily assign the new date to the medication
    3. If they click "Undo" then we revert back
    4. If they do not click "Undo" then we save that medication with the new date to firestore
    which we already have a function for


     */


     private fun clearUndoState(medication: Medication) {
         val medicationSchedule = medication.schedule
         if(medicationSchedule is MedicationSchedule.Daily) {
             _lastOriginalMedication = medication
             _lastOriginalDates = medicationSchedule.nextDueDates.toList()
         }

    }


    fun testStateTracking(medication: Medication) {
        val medicationSchedule = medication.schedule

        if (medicationSchedule is MedicationSchedule.Daily) {
            _lastOriginalDates = medicationSchedule.nextDueDates.toList()
            _lastOriginalMedication = medication
            Log.d("UNDO_TEST", "Stored dates: $_lastOriginalDates")
            Log.d("UNDO_TEST", "Stored medication: ${_lastOriginalMedication?.name}")
        } else {
            Log.d("UNDO_TEST","Not a daily schedule, ignoring for now")
        }

    }

        fun markWithUndoPrep(medication: Medication): Medication {
        val medicationSchedule = medication.schedule
        if (medicationSchedule is MedicationSchedule.Daily) {

            _lastOriginalDates = medicationSchedule.nextDueDates.toList()
            _lastOriginalMedication = medication
            return debugAdvanceDates(medication) // From previous step

        } else {
            return medication
        }

    }
    fun undoLastTaken(medication: Medication) {
        _lastOriginalMedication?.let { originalMed ->
            _lastOriginalDates?.let { originalDates ->
                // Create reverted copy
                val revertedMed = originalMed.copy(
                    schedule = (originalMed.schedule as MedicationSchedule.Daily).copy(
                        nextDueDates = originalDates
                    )
                )
                Log.d("UndoLastTaken","Reverted medication dates: $originalDates")

                _medications.value = _medications.value?.map {
                    if (it.id == originalMed.id) revertedMed else it
                }
            }
        }
        clearUndoState(medication)
    }



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




    fun getCurrentDay() {

        val today = LocalDate.now()

        Log.d("HomeViewModel","Current date: $today and the day is ${today.dayOfWeek}")

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
                    Log.e("HomeViewModel","Error marking ${medication.name} as taken")
                    }
                }
            }
        }
    }

    fun testDateAdvanceMedication(userId: String, medication: Medication) {
        val currentDate = LocalDate.now()

        viewModelScope.launch {
            when(medication.schedule) {
                is MedicationSchedule.WeeklySchedule -> {
                    val myList: MutableList<LocalDateTime> = medication.schedule.nextDueDates.toMutableList()

                    medication.schedule.nextDueDates.forEach { date ->
                        if (date.toLocalDate() <= currentDate) {
                            Log.d("TEST_DATE_ADVANCE", "Advancing week by 1 for $date")

                            // Remove the old date and add the new advanced date
                            myList.remove(date)
                            val newDate = date.plusWeeks(1)
                            myList.add(newDate)

                            // Update the medication dates in Firestore
                            medication.id?.let {
                                val success = FireStoreRepository.updateMedicationDates(userId, it, myList)

                                if (success) {
                                    Log.d("TEST_DATE_ADVANCE", "Successfully updated dates for medication: ${medication.name}")
                                } else {
                                    Log.e("TEST_DATE_ADVANCE", "Error updating dates for medication: ${medication.name}")
                                }
                            }

                            Log.d("TEST_DATE_ADVANCE", "New Date: $newDate")
                        } else {
                            Log.d("TEST_DATE_ADVANCE", "Not updating date: $date")
                        }
                    }
                }
                else -> {
                    Log.d("TEST_DATE_ADVANCE", "Medication ${medication.name} is not a weekly medication")
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
