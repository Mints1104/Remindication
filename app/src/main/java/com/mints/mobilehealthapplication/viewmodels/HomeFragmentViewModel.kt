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
import java.time.LocalTime

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

    private fun getDatesBetween(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var date = startDate.plusDays(1)

        while (!date.isAfter(endDate)) {
            dates.add(date)
            date = date.plusDays(1) // Move to the next day
        }

        return dates
    }

    fun testCheckingDatesInPast(userId: String) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val currentDate = LocalDate.now()
            val currentTime = LocalTime.now()

            Log.d("Test", "Current day: ${now.dayOfWeek}")
            Log.d("Test", "Current time: $currentTime")

            val pastDate = LocalDate.of(2024, 12, 1)
            val dateList = getDatesBetween(pastDate, currentDate)
            Log.d("Test", "Dates between past date and present: $dateList")

            _medications.value?.forEach { medication ->
                when (val schedule = medication.schedule) {
                    is MedicationSchedule.Daily -> {
                        // Filter out only the missed dates
                        val missedDueDates = schedule.nextDueDates.filter { it.toLocalDate().isBefore(currentDate) }
                        if (missedDueDates.isNotEmpty()) {
                            Log.d("Test", "Next date for ${medication.name} is behind current date.")
                            val missedEvents = mutableListOf<MedicationEvent>()

                            // Loop only through the missed due dates
                            missedDueDates.forEach { time ->
                                Log.d("Test", "Scheduled medication date: ${time.toLocalDate()}")

                                val medDatesList = getDatesBetween(time.toLocalDate(), currentDate.minusDays(1))
                                Log.d("Test", "Dates between scheduled date and yesterday: ${medDatesList.size}")

                                medDatesList.forEach { date ->
                                    val dateTime = date.atTime(time.toLocalTime())
                                    Log.d("Test", "Marking as missed: $dateTime")
                                    medication.markAsMissed(dateTime)
                                    missedEvents.add(MedicationEvent.Missed(date = dateTime))
                                }

                                // Update the next due date until it's in the future
                                var newTime = time
                                while (newTime.isBefore(now)) {
                                    newTime = newTime.plusDays(1)
                                }
                                Log.d("Test", "New due date for ${medication.name}: $newTime")

                                medication.id?.let { medId ->
                                    val success = FireStoreRepository.updateMedicationDates(
                                        userId = userId,
                                        medicationId = medId,
                                        newDates = listOf(newTime)
                                    )
                                    if (success) {
                                        Log.d("Test", "Date successfully updated!")
                                    } else {
                                        Log.e("Test", "Date failed to update :(")
                                    }
                                }
                            }

                            // Batch update the missed events in Firestore
                            if (missedEvents.isNotEmpty() && medication.id != null) {
                                val updateSuccess = FireStoreRepository.updateMultipleMedicationHistories(
                                    userId = userId,
                                    medicationId = medication.id!!,
                                    events = missedEvents
                                )
                                if (updateSuccess) {
                                    Log.d("Test", "Medication history updated with missed events!")
                                } else {
                                    Log.e("Test", "Failed to update medication history with missed events.")
                                }
                            }
                        } else {
                            Log.d("Test", "Next date for ${medication.name} is AFTER current date.")
                        }
                    }
                    // Additional schedule types can be added here later
                    else -> {}
                }
            }
        }
    }


    /*

     fun testCheckingDatesInPast(userId: String) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val day = now.dayOfWeek
            val currentTime = LocalTime.now()
            val currentDate = LocalDate.now()

            Log.d("Test", "Current day: $day")
            Log.d("Test", "Current time: $currentTime")
            val d = LocalDate.of(2024, 12, 1) // Example past date

            val dateList = getDatesBetween(d, currentDate)

            Log.d("Test", "Dates between past date and present: $dateList")

            _medications.value?.forEach { medication ->
                when (val schedule = medication.schedule) {

                    is MedicationSchedule.Daily -> {
                        val missedDueDates = schedule.nextDueDates.filter { it.isBefore(now) }

                        if (missedDueDates.isNotEmpty()) {
                            Log.d(
                                "Test",
                                "Next date for ${medication.name} is behind current date."
                            )
                            schedule.nextDueDates.forEach { time ->
                                //     Log.d("Test","Time of medication: ${time.toLocalTime()}")
                                Log.d("Test", "Date of medication: ${time.toLocalDate()}")
                                val medDatesList =
                                    getDatesBetween(time.toLocalDate(), currentDate.minusDays(1))
                                Log.d(
                                    "Test",
                                    "Dates Between current date and now ${medDatesList.size}"
                                )


                                medDatesList.forEach { date ->
                                    val dateTime = date.atTime(time.toLocalTime())
                                    Log.d("Test", "Date to mark as missed: $dateTime")
                                    medication.markAsMissed(dateTime)

                                    medication.id?.let {
                                        FireStoreRepository.updateMedicationHistory(
                                            userId = userId,
                                            medicationId = it,
                                            event = MedicationEvent.Missed(date = dateTime)
                                        )
                                    }
                                }

                                if (time.toLocalTime() < currentTime) {
                                    val newTime = time.plusDays(1)
                                    Log.d("Test", "New date: $newTime")

                                    medication.id?.let { medId ->
                                        val success = FireStoreRepository.updateMedicationDates(
                                            userId = userId,
                                            medicationId = medId,
                                            newDates = listOf(newTime)
                                        )

                                        if(success) {
                                            Log.d("Test","Date successfully updated!")
                                        } else {
                                            Log.e("Test","Date failed to update :(")
                                        }

                                    }

                                } else {
                                    val dateTime: LocalDateTime =
                                        LocalDateTime.of(currentDate, time.toLocalTime())
                                    Log.d("Test", "New date: $dateTime")

                                }




                            }

                        } else {
                            Log.d("Test", "Next date for ${medication.name} is AFTER current date.")
                        }
                    }

                    is MedicationSchedule.WeeklySchedule -> {
                        val missedDueDates = schedule.nextDueDates.filter { it.isBefore(now) }
                        if (missedDueDates.isNotEmpty()) {
                            Log.d(
                                "Test",
                                "Next date for ${medication.name} is behind current date."
                            )

                        } else {
                            Log.d("Test", "Next date for ${medication.name} is AFTER current date.")
                        }
                    }

                    else -> {}

                }
            }
        }
    }

     */


    private fun checkAnyDatesInPast(userId: String) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            _medications.value?.forEach { medication ->
                when (val schedule = medication.schedule) {
                    is MedicationSchedule.Daily -> {
                        val missedDueDates = schedule.nextDueDates.filter {
                            it.isBefore(now)  // Compare with current time instead of just the date
                        }

                        if (missedDueDates.isNotEmpty() &&
                            !medication.medicationHistory.wasTakenToday() &&
                            !medication.medicationHistory.wasSkippedToday()
                        ) {
                            val missedDateTime = missedDueDates.minOf { it }
                            medication.markAsMissed(dateTime = missedDateTime)
                            medication.id?.let {
                                FireStoreRepository.updateMedicationHistory(
                                    userId = userId,
                                    medicationId = it,
                                    event = MedicationEvent.Missed(date = missedDateTime)
                                )
                            }
                        }

                        val updatedDates = schedule.nextDueDates.map { dueDate ->
                            if (dueDate.isBefore(now)) dueDate.plusDays(1) else dueDate
                        }

                        medication.id?.let { medId ->
                            val success = FireStoreRepository.updateMedicationDates(
                                userId = userId,
                                medicationId = medId,
                                newDates = updatedDates
                            )

                            if (success) {

                                Log.d(
                                    "Test",
                                    "Successfully advanced schedule for medication: ${medication.name}"
                                )
                            } else {
                                Log.e(
                                    "Test",
                                    "Failed to advance schedule for medication: ${medication.name}"
                                )
                            }
                        }
                    }

                    is MedicationSchedule.WeeklySchedule -> {
                        Log.d(
                            "Test",
                            "Original weekly dates for ${medication.name}: ${schedule.nextDueDates}"
                        )

                        val missedDueDates = schedule.nextDueDates.filter {
                            it.isBefore(now)
                        }

                        if (missedDueDates.isNotEmpty() &&
                            !medication.medicationHistory.wasTakenToday() &&
                            !medication.medicationHistory.wasSkippedToday()
                        ) {
                            // Use the earliest missed due date as the missed time
                            val missedDateTime = missedDueDates.minOf { it }
                            Log.d(
                                "Test",
                                "${medication.name} was not taken/skipped for time: $missedDateTime, marking as missed"
                            )
                            medication.markAsMissed(dateTime = missedDateTime)
                            medication.id?.let {
                                FireStoreRepository.updateMedicationHistory(
                                    userId = userId,
                                    medicationId = it,
                                    event = MedicationEvent.Missed(date = missedDateTime)
                                )
                            }
                        }

                        if (missedDueDates.isNotEmpty()) {
                            val updatedDates = schedule.nextDueDates.map { dueDate ->
                                if (dueDate.isBefore(now)) {
                                    dueDate.plusDays(7)
                                } else {
                                    dueDate
                                }
                            }.sorted()

                            medication.id?.let { medId ->
                                val success = FireStoreRepository.updateMedicationDates(
                                    userId = userId,
                                    medicationId = medId,
                                    newDates = updatedDates
                                )

                                if (success) {

                                    Log.d(
                                        "Test",
                                        "Successfully advanced weekly schedule for medication: ${medication.name}"
                                    )
                                } else {
                                    Log.e(
                                        "Test",
                                        "Failed to advance weekly schedule for medication: ${medication.name}"
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Log.d(
                            "Test",
                            "Schedule type not handled for medication: ${medication.name}"
                        )
                    }
                }
            }
        }
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

    fun invalidateCache() {
        _medications.value = emptyList()

    }

    fun getMedications(uid: String, onComplete: () -> Unit = {}) {
        Log.d("HomeFragmentViewModel", "Starting real-time medication listener for user: $uid")

        FireStoreRepository.getMedicationsSnapshot(uid) { meds, error ->
            if (error != null) {
                Log.e("HomeFragmentVM", "Failed to get medications: ${error.message}")
                onComplete() // Even on error, signal completion
                return@getMedicationsSnapshot
            }

            Log.d("HomeFragmentViewModel", "Fetched ${meds.size} medications from snapshot")
            _medications.postValue(meds) // Update LiveData with the new list
            onComplete()
        }
    }


   /* fun getMedications(uid: String, onComplete: () -> Unit = {}) {
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
    }*/





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
        val currentDateTime = LocalDateTime.now()
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
                   medication.markAsTaken(dateTime = currentDateTime)

                   //Update the medication history in FireStore

                   val success = FireStoreRepository.updateMedicationHistory(
                       userId = userId,
                       medicationId = medId,
                       event = MedicationEvent.Taken(date = currentDateTime)
                   )

                 if(success) {
                     //Update local list
                     _medications.value = _medications.value?.map {
                         if(it.id == medication.id) medication else it
                     }
                     NotificationHelper(notificationHelper.getContext()).cancelBackupNotification(medication.name)

                     val streakUpdated = FireStoreRepository.updateAdherenceStreak(userId)
                     if (!streakUpdated) {
                         Log.e("HomeViewModel", "Failed to update adherence streak.")
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
                        NotificationHelper(notificationHelper.getContext()).cancelBackupNotification(medication.name)

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
