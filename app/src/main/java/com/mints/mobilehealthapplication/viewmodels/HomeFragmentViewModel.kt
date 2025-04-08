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

    private val _todaysMedications = MutableLiveData<List<Medication>>()
    val todaysMedications: LiveData<List<Medication>> get() = _todaysMedications



    private val _navigateToDetails = MutableLiveData<Medication?>()
    val navigateToDetails: LiveData<Medication?> get() = _navigateToDetails

    private var _lastOriginalMedication: Medication? = null
    private var _lastOriginalDates: List<LocalDateTime>? = null

    val lastOriginalDates: List<LocalDateTime>? get() = _lastOriginalDates

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



    fun invalidateCache() {
        _medications.value = emptyList()

    }

    fun getMedications(uid: String, onComplete: () -> Unit = {}) {
        Log.d("HomeFragmentViewModel", "Starting real-time medication listener for user: $uid")

        FireStoreRepository.getMedicationsSnapshot(uid) { meds, error ->
            if (error != null) {
                Log.e("HomeFragmentVM", "Failed to get medications: ${error.message}")
                onComplete()
                return@getMedicationsSnapshot
            }

            Log.d("HomeFragmentViewModel", "Fetched ${meds.size} medications from snapshot")
            _medications.postValue(meds)
            onComplete()
        }
    }



    fun deleteMedication(uid: String, medicationId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                FireStoreRepository.deleteMedication(uid, medicationId)
                onComplete()
            } catch (e: Exception) {
                Log.e("HomeFragmentVM", "Delete failed: ${e.message}")
            }
        }
    }


    fun getCurrentDay() {

        val today = LocalDate.now()

        Log.d("HomeViewModel","Current date: $today and the day is ${today.dayOfWeek}")

    }




    fun markMedicationAsTaken(userId: String, medication: Medication, onComplete: () -> Unit) {
        val currentDateTime = LocalDateTime.now()
        val today = LocalDate.now()
        viewModelScope.launch {
            medication.id?.let { medId ->
                try {
                    val eventDateTime = when (val schedule = medication.schedule) {
                        is MedicationSchedule.Daily -> schedule.nextDueDates.firstOrNull()
                        is MedicationSchedule.WeeklySchedule -> schedule.nextDueDates.firstOrNull()
                        is MedicationSchedule.Cyclic -> schedule.nextDueDates.firstOrNull { it.toLocalDate() == today }
                        else -> LocalDateTime.now()
                    } ?: LocalDateTime.now()

                    medication.markAsTaken(dateTime = currentDateTime)
                    Log.d("HomeViewModel", "Marking as taken at: $currentDateTime")
                    val lastEvent =  medication.medicationHistory.getLastEvent()
                    if (lastEvent != null) {
                        Log.d("HomeViewModel", "Last event date: ${lastEvent.date}")
                    }

                    val success = FireStoreRepository.updateMedicationHistory(
                        userId = userId,
                        medicationId = medId,
                        event = MedicationEvent.Taken(
                            instant = currentDateTime.atZone(ZoneId.systemDefault()).toInstant()
                        )
                    )

                    val success2 = FireStoreRepository.addMedicationEvent(userId = userId,
                        medicationId = medId,
                        event = MedicationEvent.Taken(
                            instant = eventDateTime.atZone(ZoneId.systemDefault()).toInstant()
                        )
                    )

                    if (success) {
                        _medications.value = _medications.value?.map {
                            if (it.id == medication.id) medication else it
                        }
                        NotificationHelper(notificationHelper.getContext()).cancelBackupNotification(medication.name)
                        val dueTimeMillis = eventDateTime.atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                        NotificationHelper(notificationHelper.getContext())
                            .cancelRegularNotification(medication.name, dueTimeMillis)
                        val streakUpdated = FireStoreRepository.updateAdherenceStreak(userId)
                        if (!streakUpdated) {
                            Log.e("HomeViewModel", "Failed to update adherence streak.")
                        } else {
                            Log.d("HomeViewModel","Successfully updated adherence streak")
                        }
                    } else {
                        Log.e("HomeViewModel", "Error marking ${medication.name} as taken")
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Exception marking medication as taken", e)
                }
            }
            onComplete()
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

    fun markMedicationAsSkipped(userId: String, medication: Medication, onComplete: () -> Unit) {
        val today = LocalDate.now()

        viewModelScope.launch {
            medication.id?.let {medId ->
                try {

                    val eventDateTime = when(val schedule = medication.schedule) {
                        is MedicationSchedule.Daily -> schedule.nextDueDates.firstOrNull()
                        is MedicationSchedule.WeeklySchedule -> schedule.nextDueDates.firstOrNull()
                        is MedicationSchedule.Cyclic -> schedule.nextDueDates.firstOrNull { it.toLocalDate() == today }

                        else -> LocalDateTime.now()
                    } ?: LocalDateTime.now()

                    medication.markAsSkipped(dateTime = eventDateTime)



                    val success2 = FireStoreRepository.updateMedicationHistory(
                        userId = userId,
                        medicationId = medId,
                        event = MedicationEvent.Skipped(
                            instant = eventDateTime.atZone(ZoneId.systemDefault()).toInstant()
                        )
                    )
                    val success = FireStoreRepository.addMedicationEvent(userId = userId,
                        medicationId = medId,
                        event = MedicationEvent.Skipped(
                            instant = eventDateTime.atZone(ZoneId.systemDefault()).toInstant()
                        )
                    )

                    if(success) {
                        _medications.value = _medications.value?.map {
                            if (it.id == medication.id) medication else it
                        }
                        NotificationHelper(notificationHelper.getContext()).cancelBackupNotification(medication.name)
                        val dueTimeMillis = eventDateTime.atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                        NotificationHelper(notificationHelper.getContext())
                            .cancelRegularNotification(medication.name, dueTimeMillis)
                    } else {
                        Log.e("HomeViewModel","Error marking ${medication.name} as skipped")
                    }
                } catch(e:Exception) {
                    Log.e("HomeViewModel","Exception marking medication as skipped",e)
                }
            }
            onComplete()

        }
    }

        fun getMedicationList(): MutableLiveData<List<Medication>> = _medications


        fun onMedicationClicked(medication: Medication) {
            _navigateToDetails.value = medication
        }

    }
