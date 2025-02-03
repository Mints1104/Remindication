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




    fun getMedications(uid: String, onComplete: () -> Unit = {}) {
        // Check if medications are already cached to avoid re-fetching
        Log.d("HomeFragmentViewModel", "Checking cached medications: ${_medications.value}")

        if (_medications.value.isNullOrEmpty()) {
            Log.d("HomeFragmentViewModel", "Medications cache is empty, fetching medications for user: $uid")

            viewModelScope.launch {
                try {
                    Log.d("HomeFragmentViewModel", "Fetching medications from Firestore...")

                    val meds = FireStoreRepository.getMedications(uid)
                    Log.d("HomeFragmentViewModel", "Fetched ${meds.size} medications")

                    // Log the fetched medications to inspect the data
                    meds.forEach { medication ->
                        Log.d("HomeFragmentViewModel", "Fetched medication: ${medication.name}")
                    }

                    _medications.postValue(meds) // Cache medications

                    Log.d("HomeFragmentViewModel", "Medications cached successfully.")
                    onComplete() // Notify that the fetch is complete

                } catch (e: Exception) {
                    Log.e("HomeFragmentVM", "Failed to get medications: ${e.message}")
                    // Optionally, update an error state or LiveData to notify UI of failure
                    onComplete() // Notify even on failure
                }
            }
        } else {
            Log.d("HomeFragmentViewModel", "Medications already cached, skipping fetch.")
            onComplete() // Notify completion even if data is already cached
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







    fun getCurrentDay() {

        val today = LocalDate.now()

        Log.d("HomeViewModel","Current date: $today and the day is ${today.dayOfWeek}")

    }




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


    fun testReceivingMedicationHistory(medication: Medication) {
        viewModelScope.launch {
            val history = medication.medicationHistory
            Log.d("MED_TEST", "History: $history")
            history.getLastEventOfType(MedicationEvent.EventType.TAKEN)?.let { lastTaken ->
                Log.d("MedicationHistory", "Last taken: ${lastTaken.date}")
            }

            val compliance = history.getComplianceRate()
            Log.d("MedicationHistory", "Compliance rate: $compliance%")

            if (history.wasTakenToday()) {
                Log.d("MedicationHistory", "Medication already taken today")
            }

            val recentEvents = history.getEventsFromLastDays(7)
            Log.d("MedicationHistory", "Events in last 7 days: ${recentEvents.size}")
        }
    }

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

        fun getMedicationList(): MutableLiveData<List<Medication>> = _medications


        fun onMedicationClicked(medication: Medication) {
            _navigateToDetails.value = medication // Trigger navigation
        }




    }
