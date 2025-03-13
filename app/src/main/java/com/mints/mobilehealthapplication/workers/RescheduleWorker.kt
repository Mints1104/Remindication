package com.mints.mobilehealthapplication.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.utils.ScheduleHelper
import java.time.LocalDateTime
import java.time.ZoneId

class RescheduleWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationHelper = NotificationHelper(applicationContext)
    private val tag = "RescheduleWorker"

    override suspend fun doWork(): Result {
        return try {
            Log.d(tag, "Running RescheduleWorker after reboot")
            MidnightWorker.initialize(applicationContext)
            val userId = FireStoreRepository.getUser()?.uid ?: ""
            val medications = FireStoreRepository.getMedications(userId)
            val now = LocalDateTime.now()

            medications.forEach { medication ->
                when (val schedule = medication.schedule) {
                    is MedicationSchedule.Daily -> {
                        processDailySchedule(medication, schedule, now, userId)
                    }
                    is MedicationSchedule.WeeklySchedule -> {
                        processWeeklySchedule(medication, schedule, now, userId)
                    }
                    is MedicationSchedule.Cyclic -> {
                        processCyclicSchedule(medication, schedule, now, userId)
                    }
                    else -> {
                        Log.d(TAG, "Medication ${medication.name} has no schedule")
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in RescheduleWorker: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun processCyclicSchedule(
        medication: Medication,
        schedule: MedicationSchedule.Cyclic,
        now: LocalDateTime,
        userId: String
    ) {
        Log.d(tag, "Processing cyclic schedule for ${medication.name}")

        // Check if the medication has past due dates (missed doses)
        val missedDueDates = schedule.nextDueDates.filter { it.isBefore(now) }
        if (missedDueDates.isNotEmpty() && !medication.medicationHistory.hadEventYesterday()) {
            val missedEvents = mutableListOf<MedicationEvent>()
            missedDueDates.forEach { missedDateTime ->
                val missedDates = ScheduleHelper.getDatesBetween(
                    start = missedDateTime.toLocalDate(),
                    end = now.toLocalDate().minusDays(1)
                )
                missedDates.forEach { date ->
                    val eventDateTime = date.atTime(missedDateTime.toLocalTime())
                    Log.d(tag, "Marking ${medication.name} as missed at $eventDateTime")
                    medication.markAsMissed(eventDateTime)
                    missedEvents.add(MedicationEvent.Missed(date = eventDateTime))
                }
            }

            if (missedEvents.isNotEmpty() && medication.id != null) {
                val updateSuccess = FireStoreRepository.updateMultipleMedicationHistories(
                    userId = userId,
                    medicationId = medication.id!!,
                    events = missedEvents
                )
                if (updateSuccess) {
                    Log.d(tag, "Medication history updated with missed events for ${medication.name}")
                } else {
                    Log.e(tag, "Failed to update medication history for ${medication.name}")
                }
            }
        }

        // Determine new due dates for the cycle
        val newDueDates = ScheduleHelper.calculateCyclicDueDates(
            intakeDays = schedule.intakeDays,
            pauseDays = schedule.pauseDays,
            times = schedule.times,
            currentCycleStartDate = schedule.currentCycleStartDate
        )

        // Update Firestore with new due dates
        medication.id?.let { medId ->
            val success = FireStoreRepository.updateMedicationDates(
                userId = userId,
                medicationId = medId,
                newDates = newDueDates
            )
            if (success) {
                val nextDueTimeMillis = newDueDates.minByOrNull { it }
                    ?.atZone(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli() ?: 0L
                Log.d(tag, "Scheduling notification for ${medication.name} at $nextDueTimeMillis")
                notificationHelper.scheduleNotification(medication.name, nextDueTimeMillis)
                Log.d(tag, "Successfully advanced cyclic schedule for ${medication.name}")
            } else {
                Log.e(tag, "Failed to update cyclic schedule for ${medication.name}")
            }
        }
    }


    private suspend fun processDailySchedule(
        medication: Medication,
        schedule: MedicationSchedule.Daily,
        now: LocalDateTime,
        userId: String
    ) {
        Log.d(tag, "Original daily due dates for ${medication.name}: ${schedule.nextDueDates}")

        // Identify missed due dates.
        val missedDueDates = schedule.nextDueDates.filter { it.isBefore(now) }
        if (missedDueDates.isNotEmpty() && !medication.medicationHistory.hadEventYesterday()) {
            val missedEvents = mutableListOf<MedicationEvent>()

            missedDueDates.forEach { missedDateTime ->
                // Use the shared ScheduleHelper function.
                val missedDates = ScheduleHelper.getDatesBetween(
                    start = missedDateTime.toLocalDate(),
                    end = now.toLocalDate().minusDays(1)
                )
                missedDates.forEach { date ->
                    val eventDateTime = date.atTime(missedDateTime.toLocalTime())
                    Log.d(tag, "Marking ${medication.name} as missed at $eventDateTime")
                    medication.markAsMissed(eventDateTime)
                    missedEvents.add(MedicationEvent.Missed(date = eventDateTime))
                }
            }

            if (missedEvents.isNotEmpty() && medication.id != null) {
                val updateSuccess = FireStoreRepository.updateMultipleMedicationHistories(
                    userId = userId,
                    medicationId = medication.id!!,
                    events = missedEvents
                )
                if (updateSuccess) {
                    Log.d(tag, "Medication history updated with missed events for ${medication.name}")
                } else {
                    Log.e(tag, "Failed to update medication history for ${medication.name}")
                }
            }
        }

        // Adjust due dates using ScheduleHelper.
        val updatedDates = schedule.nextDueDates.map { dueDate ->
            if (dueDate.isBefore(now)) ScheduleHelper.adjustDailyDueDate(dueDate, now) else dueDate
        }.distinct()

        medication.id?.let { medId ->
            val success = FireStoreRepository.updateMedicationDates(
                userId = userId,
                medicationId = medId,
                newDates = updatedDates
            )
            if (success) {
                val nextDueTimeMillis = updatedDates.minByOrNull { it }
                    ?.atZone(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli() ?: 0L
                Log.d(tag, "Scheduling notification for ${medication.name} at $nextDueTimeMillis")
                notificationHelper.scheduleNotification(medication.name, nextDueTimeMillis)
                Log.d(tag, "Successfully advanced daily schedule for ${medication.name}")
            } else {
                Log.e(tag, "Failed to update daily schedule for ${medication.name}")
            }
        }
    }

    private suspend fun processWeeklySchedule(
        medication: Medication,
        schedule: MedicationSchedule.WeeklySchedule,
        now: LocalDateTime,
        userId: String
    ) {
        Log.d(tag, "Original weekly due dates for ${medication.name}: ${schedule.nextDueDates}")

        // Handle missed weekly due dates.
        val missedDueDates = schedule.nextDueDates.filter { it.isBefore(now) }
        if (missedDueDates.isNotEmpty() && !medication.medicationHistory.hadEventYesterday()) {
            val missedDateTime = missedDueDates.minByOrNull { it }!!
            Log.d(tag, "${medication.name} missed at $missedDateTime, marking as missed")
            medication.markAsMissed(missedDateTime)
            medication.id?.let {
                FireStoreRepository.updateMedicationHistory(
                    userId = userId,
                    medicationId = it,
                    event = MedicationEvent.Missed(date = missedDateTime)
                )
            }
        }

        // Adjust weekly due dates using ScheduleHelper.
        val updatedDates = schedule.nextDueDates.map { dueDate ->
            if (dueDate.isBefore(now)) ScheduleHelper.adjustWeeklyDueDate(dueDate, now) else dueDate
        }.sorted()

        medication.id?.let { medId ->
            val success = FireStoreRepository.updateMedicationDates(
                userId = userId,
                medicationId = medId,
                newDates = updatedDates
            )
            if (success) {
                val nextDueTimeMillis = updatedDates.first()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                notificationHelper.scheduleNotification(medication.name, nextDueTimeMillis)
                Log.d(tag, "Successfully advanced weekly schedule for ${medication.name}")
            } else {
                Log.e(tag, "Failed to update weekly schedule for ${medication.name}")
            }
        }
    }


    companion object {
        private const val TAG = "RescheduleWorker"
    }
}
