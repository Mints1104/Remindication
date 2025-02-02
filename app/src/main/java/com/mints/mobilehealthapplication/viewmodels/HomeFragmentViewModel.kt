package com.mints.mobilehealthapplication.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.workers.MidnightWorker
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HomeFragmentViewModel(private val notificationHelper: NotificationHelper) : ViewModel() {

     private val _medications = MutableLiveData<List<Medication>>()
    val medications: LiveData<List<Medication>> get() = _medications
    // Add context parameter

    private val _todaysMedications = MutableLiveData<List<Medication>>()
    val todaysMedications: LiveData<List<Medication>> get() = _todaysMedications



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
    init {
        MidnightWorker.initialize(notificationHelper.getContext())
    }



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
            return debugAdvanceDates(medication)

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


    fun getMedicationDetails(uid: String, medicationId: String) {
        Log.d("HomeFragmentViewModel","Fetching medication: $medicationId")

        viewModelScope.launch {
            val med = FireStoreRepository.getMedicationDetails(uid,medicationId)
            Log.d("HomeFragmentViewModel", "Fetched ${med.name}")

        }
    }



    fun updateMedicationHistory(uid: String, medicationId: String, event: MedicationEvent) {
        viewModelScope.launch {
            try {
                val success = FireStoreRepository.updateMedicationHistory(
                    userId = uid,
                    medicationId = medicationId,
                    event = event
                )
                if (success) {
                    // Update the local medication list to reflect the new history
                    _medications.value = _medications.value?.map { medication ->
                        if (medication.id == medicationId) {
                            // Create a new medication history with the added event
                            val updatedHistory = medication.medicationHistory.apply {
                                addEvent(event)
                            }
                            // Return the medication with updated history
                            medication.copy(medicationHistory = updatedHistory)
                        } else {
                            medication
                        }
                    }
                    Log.d("HomeViewModel", "Successfully updated medication history for $medicationId")
                } else {
                    Log.e("HomeViewModel", "Failed to update medication history for $medicationId")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error updating medication history: ${e.message}")
            }
        }
    }

    fun getMedications(uid: String, onComplete: () -> Unit = {}) {
        Log.d("HomeFragmentViewModel", "Fetching medications for user: $uid")
        viewModelScope.launch {
            try {
            val meds = FireStoreRepository.getMedications(uid)
            Log.d("HomeFragmentViewModel", "Fetched ${meds.size} medications")
            _medications.postValue(meds)
            onComplete()
        } catch(e:Exception) {
                Log.e("HomeFragmentVM", "Failed to get medications: ${e.message}")


            }
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

    private fun getClosestDate(currentList: List<Medication>) {


        // Filter daily schedules
        val dailySchedList = currentList.filter { it.schedule is MedicationSchedule.Daily }

        // Get the current time
        val now = LocalDateTime.now()

        // Find the medication with the earliest upcoming date
        val closestMedication = dailySchedList
            .filter { it.schedule is MedicationSchedule.Daily }
            .minByOrNull { medication ->
                val schedule = medication.schedule as MedicationSchedule.Daily
                val closestDate = schedule.nextDueDates
                    .filter { it.isAfter(now) }
                    .minOrNull()
                closestDate ?: LocalDateTime.MAX
            }

        if (closestMedication != null) {
            val schedule = closestMedication.schedule as MedicationSchedule.Daily
            val closestDueDate = schedule.nextDueDates.filter { it.isAfter(now) }.minOrNull()

        }
    }


    private fun debugAdvanceDates(medication: Medication): Medication {
        val now = LocalDateTime.now()
        return when (val schedule = medication.schedule) {
            is MedicationSchedule.Daily -> {
                // Handle "Twice Daily" separately
                if (medication.schedule.frequencyType == "Twice Daily") {
                    val closestDate = schedule.nextDueDates
                        .filter { it.isAfter(now) }
                        .minByOrNull { it }

                    if (closestDate != null) {
                        Log.d("DEBUG_DATES", "CLOSEST DATE (TWICE DAILY): $closestDate")

                        // Calculate the new date (add 1 day to the closest date)
                        val newDate = closestDate.plusDays(1)
                        Log.d("DEBUG_DATES", "CLOSEST DATE UPDATED (TWICE DAILY): $newDate")

                        // Replace the old closest date with the new one in the list
                        val newDates = schedule.nextDueDates.toMutableList()
                        newDates[newDates.indexOf(closestDate)] = newDate  // Update the closest date

                        // Update the medication schedule with the new list of dates
                        return medication.copy(
                            schedule = schedule.copy(nextDueDates = newDates)
                        )
                    } else {
                        Log.d("DEBUG_DATES", "No upcoming dates found for this medication.")
                    }
                }

                // Handle the general "Daily" case (after Twice Daily if applicable)
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

    private fun scheduleNextNotification(medication: Medication) {
        when (val schedule = medication.schedule) {
            is MedicationSchedule.Daily -> {
                val nextDueTimeMillis = schedule.nextDueDates[0]
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                notificationHelper.scheduleNotification(
                    medication.name,
                    medication.dosage,
                    nextDueTimeMillis
                )
            }
            is MedicationSchedule.WeeklySchedule -> {
                val nextDueTimeMillis = schedule.nextDueDates[0]
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                notificationHelper.scheduleNotification(
                    medication.name,
                    medication.dosage,
                    nextDueTimeMillis
                )
            }
            else -> Log.d("HomeViewModel", "Schedule type not supported for notifications")
        }
    }

    /*
    User marks medication as taken should update the history saying the medication was taken
    If the user did not take the medication and it hit midnight then that medication should be
    set to missed
     */

    fun markMedicationAsTaken(userId: String, medication: Medication) {
        viewModelScope.launch {
            medication.id?.let { medId ->
               try {
                   //First mark the medication as taken locally
                   val eventDateTime = when(val schedule = medication.schedule) {
                       is MedicationSchedule.Daily -> schedule.nextDueDates.firstOrNull()
                       is MedicationSchedule.WeeklySchedule -> schedule.nextDueDates.firstOrNull()
                       else -> LocalDateTime.now()
                   } ?: LocalDateTime.now()
                   //Update the local medication object
                   medication.markAsTaken(dateTime = eventDateTime)

                   //Update the medication history in FireStore

                   val success = FireStoreRepository.updateMedicationHistory(
                       userId = userId,
                       medicationId = medId,
                       event = MedicationEvent.Taken(date = eventDateTime)
                   )

                 if(success) {
                     //Update local list
                     _medications.value = _medications.value?.map {
                         if(it.id == medication.id) medication else it
                     }

                 } else {
                     Log.e("HomeViewModel","Error marking ${medication.name} as taken")
                 }

                   } catch(e:Exception) {
                       Log.e("HomeViewModel","Exception marking medication as taken",e)
                   }
               }
        }
    }

    /*fun markMedicationAsTaken(userId: String, medication: Medication) {
        viewModelScope.launch {
            medication.id?.let { medId ->
                if (medication.schedule is MedicationSchedule.Daily) {

                    medication.markAsTaken(dateTime = medication.schedule.nextDueDates[0])
                    val success = FireStoreRepository.updateMedicationDates(
                        userId = userId,
                        medicationId = medId,
                        newDates = medication.schedule.nextDueDates
                    )
                    if (success) {
                        // Update local list
                        _medications.value = _medications.value?.map {
                            if (it.id == medication.id) medication else it
                        }

                    } else {
                        Log.e("HomeViewModel", "Error marking ${medication.name} as taken")
                    }
                }

                if (medication.schedule is MedicationSchedule.WeeklySchedule) {

                    medication.markAsTaken(dateTime = medication.schedule.nextDueDates[0])
                    val success = FireStoreRepository.updateMedicationDates(
                        userId = userId,
                        medicationId = medId,
                        newDates = medication.schedule.nextDueDates
                    )
                    if (success) {
                        // Update local list
                        _medications.value = _medications.value?.map {
                            if (it.id == medication.id) medication else it
                        }

                    } else {
                        Log.e("HomeViewModel", "Error marking ${medication.name} as taken")
                    }


                }
            }
        }
    }*/
    /*
    we create an updated medication object
    we mark it as taken/skipped
    we save that up[dated value to firestore
     */

    fun markMedicationAsSkipped(userId: String, medication: Medication) {

        viewModelScope.launch {
            medication.id?.let {medId ->
                try {

                    val eventDateTime = when(val schedule = medication.schedule) {
                        is MedicationSchedule.Daily -> schedule.nextDueDates.firstOrNull()
                        is MedicationSchedule.WeeklySchedule -> schedule.nextDueDates.firstOrNull()
                        else -> LocalDateTime.now()
                    } ?: LocalDateTime.now()

                    //Mark medication as skipped locally with the correct due date
                    medication.markAsSkipped(dateTime = eventDateTime)

                    val success = FireStoreRepository.updateMedicationHistory(
                        userId = userId,
                        medicationId = medId,
                        event = MedicationEvent.Skipped(date = eventDateTime)
                    )

                    if(success) {
                        //Update local list
                        _medications.value = _medications.value?.map {
                            if (it.id == medication.id) medication else it
                        }
                    } else {
                        Log.e("HomeViewModel","Error marking ${medication.name} as skipped")
                    }
                } catch(e:Exception) {
                    Log.e("HomeViewModel","Exception marking medication as skipped",e)
                }
            }
        }
    }


   /* fun markMedicationAsSkipped(userId: String, medication: Medication) {
        viewModelScope.launch {
            medication.id?.let { medId ->
                if (medication.schedule is MedicationSchedule.Daily) {

                    medication.markAsSkipped(dateTime = medication.schedule.nextDueDates[0])
                    val success = FireStoreRepository.updateMedicationDates(
                        userId = userId,
                        medicationId = medId,
                        newDates = medication.schedule.nextDueDates
                    )
                    if (success) {
                        _medications.value = _medications.value?.map {
                            if (it.id == medication.id) medication else it
                        }

                    } else {
                        Log.e("HomeViewModel", "Error marking ${medication.name} as skipped")
                    }


                }

                if (medication.schedule is MedicationSchedule.WeeklySchedule) {

                    medication.markAsSkipped(dateTime = medication.schedule.nextDueDates[0])
                    val success = FireStoreRepository.updateMedicationDates(
                        userId = userId,
                        medicationId = medId,
                        newDates = medication.schedule.nextDueDates
                    )
                    if (success) {
                        // Update local list
                        _medications.value = _medications.value?.map {
                            if (it.id == medication.id) medication else it
                        }
                    } else {
                        Log.e("HomeViewModel", "Error marking ${medication.name} as skipped")
                    }
                }
            }
        }
    }*/



    fun testDateAdvanceMedication(userId: String, medication: Medication) {
        val currentDate = LocalDate.now()

        viewModelScope.launch {
            when (medication.schedule) {
                is MedicationSchedule.WeeklySchedule -> {
                    val weeklySchedule = medication.schedule
                    val originalDates = weeklySchedule.nextDueDates
                    val updatedDates = originalDates.map { date ->
                        if (date.toLocalDate() <= currentDate) {
                            Log.d("TEST_DATE_ADVANCE", "Advancing week by 1 for $date")
                            val newDate = date.plusWeeks(1)
                            Log.d("TEST_DATE_ADVANCE", "New Date: $newDate")
                            newDate
                        } else {
                            Log.d("TEST_DATE_ADVANCE", "Not updating date: $date")
                            date
                        }
                    }.toMutableList()

                    // Check if any dates were actually updated
                    if (updatedDates != originalDates) {
                        medication.id?.let { medId ->
                            val success = FireStoreRepository.updateMedicationDates(userId, medId, updatedDates)
                            if (success) {
                                Log.d("TEST_DATE_ADVANCE", "Successfully updated dates for medication: ${medication.name}")
                            } else {
                                Log.e("TEST_DATE_ADVANCE", "Error updating dates for medication: ${medication.name}")
                            }
                        }
                    } else {
                        Log.d("TEST_DATE_ADVANCE", "No dates needed updating for ${medication.name}")
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
