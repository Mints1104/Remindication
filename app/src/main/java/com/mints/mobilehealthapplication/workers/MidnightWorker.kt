package com.mints.mobilehealthapplication.workers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.ui.HomeFragment.Companion.REFRESH_ACTION
import com.mints.mobilehealthapplication.utils.ScheduleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class MidnightWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // Instantiate NotificationHelper using the application context.
    private val notificationHelper = NotificationHelper(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val now = LocalDateTime.now()
            val userId = FireStoreRepository.getUser()?.uid ?: ""
            val medications = FireStoreRepository.getMedications(userId)

            // Process each medication based on its schedule type.
            medications.forEach { medication ->
                when (val schedule = medication.schedule) {
                    is MedicationSchedule.Daily -> {
                        processDailySchedule(medication, schedule, now, userId)
                    }
                    is MedicationSchedule.WeeklySchedule -> {
                        processWeeklySchedule(medication, schedule, now, userId)
                    }
                    else -> {
                        Log.d(TAG, "Schedule type not handled for medication: ${medication.name}")
                    }
                }
            }

            // Re-schedule MidnightWorker for the next midnight.
            scheduleNextMidnightWork(applicationContext)

            // Broadcast refresh intent for UI updates.
            val refreshIntent = Intent(REFRESH_ACTION)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(refreshIntent)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in midnight worker: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun processDailySchedule(
        medication: Medication,
        schedule: MedicationSchedule.Daily,
        now: LocalDateTime,
        userId: String
    ) {
        Log.d(TAG, "Original daily due dates for ${medication.name}: ${schedule.nextDueDates}")

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
                    Log.d(TAG, "Marking ${medication.name} as missed at $eventDateTime")
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
                    Log.d(TAG, "Medication history updated with missed events for ${medication.name}")
                } else {
                    Log.e(TAG, "Failed to update medication history for ${medication.name}")
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
                Log.d(TAG, "Scheduling notification for ${medication.name} at $nextDueTimeMillis")
                notificationHelper.scheduleNotification(medication.name, nextDueTimeMillis)
                Log.d(TAG, "Successfully advanced daily schedule for ${medication.name}")
            } else {
                Log.e(TAG, "Failed to update daily schedule for ${medication.name}")
            }
        }
    }

    private suspend fun processWeeklySchedule(
        medication: Medication,
        schedule: MedicationSchedule.WeeklySchedule,
        now: LocalDateTime,
        userId: String
    ) {
        Log.d(TAG, "Original weekly due dates for ${medication.name}: ${schedule.nextDueDates}")

        // Handle missed weekly due dates.
        val missedDueDates = schedule.nextDueDates.filter { it.isBefore(now) }
        if (missedDueDates.isNotEmpty() && !medication.medicationHistory.hadEventYesterday()) {
            val missedDateTime = missedDueDates.minByOrNull { it }!!
            Log.d(TAG, "${medication.name} missed at $missedDateTime, marking as missed")
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
                Log.d(TAG, "Successfully advanced weekly schedule for ${medication.name}")
            } else {
                Log.e(TAG, "Failed to update weekly schedule for ${medication.name}")
            }
        }
    }

    companion object {
        private const val MIDNIGHT_WORK_NAME = "midnight_work"
        private const val TAG = "MidnightWorker"

        fun initialize(context: Context) {
            Log.d(TAG, "Initializing MidnightWorker")
            scheduleNextMidnightWork(context)
        }

        private fun scheduleNextMidnightWork(context: Context) {
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val delayInMillis = nextMidnight
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli() - System.currentTimeMillis()

            val workRequest = OneTimeWorkRequestBuilder<MidnightWorker>()
                .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
                .addTag(MIDNIGHT_WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    MIDNIGHT_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
        }
    }
}
