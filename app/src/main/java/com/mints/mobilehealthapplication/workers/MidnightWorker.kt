package com.mints.mobilehealthapplication.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class MidnightWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting midnight schedule update")
            val today = LocalDate.now()
            val userId = FireStoreRepository.getUser()?.uid ?: ""
            val medications = FireStoreRepository.getMedications(userId)

            medications.forEach { medication ->
                when (val schedule = medication.schedule) {
                    is MedicationSchedule.Daily -> {
                        Log.d(TAG,"Original date for ${medication.name}: ${schedule.nextDueDates}")
                        val missedDueDates = schedule.nextDueDates.filter {
                            it.toLocalDate() <= today
                        }

                        if (missedDueDates.isNotEmpty() &&
                            !medication.medicationHistory.wasTakenToday() &&
                            !medication.medicationHistory.wasSkippedToday()
                        ) {
                            Log.d(TAG, "${medication.name} was not taken/skipped today, marking as missed")
                            medication.markAsMissed(dateTime = LocalDateTime.now())
                            medication.id?.let {
                                FireStoreRepository.updateMedicationHistory(
                                    userId = userId,
                                    medicationId = it,
                                    event = MedicationEvent.Missed(date = LocalDateTime.now())
                                )
                            }
                        }

                        val updatedDates = schedule.nextDueDates.map { dueDate ->
                            if (dueDate.toLocalDate() <= today) dueDate.plusDays(1) else dueDate
                        }

                        medication.id?.let { medId ->
                            val success = FireStoreRepository.updateMedicationDates(
                                userId = userId,
                                medicationId = medId,
                                newDates = updatedDates
                            )

                            if (success) {
                                // Schedule notification for the updated medication
                                val nextDueTimeMillis = updatedDates[0]
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                                notificationHelper.scheduleNotification(
                                    medication.name,
                                    medication.dosage,
                                    nextDueTimeMillis
                                )
                                Log.d(TAG, "Successfully advanced schedule for medication: ${medication.name}")
                            } else {
                                Log.e(TAG, "Failed to advance schedule for medication: ${medication.name}")
                            }
                        }
                    }
                    is MedicationSchedule.WeeklySchedule -> {
                        Log.d(TAG, "Original weekly dates for ${medication.name}: ${schedule.nextDueDates}")

                        // Check for missed dates that are in the past
                        val missedDueDates = schedule.nextDueDates.filter {
                            it.toLocalDate() <= today
                        }

                        // Mark medication as missed if any scheduled time was missed
                        if (missedDueDates.isNotEmpty() &&
                            !medication.medicationHistory.wasTakenToday() &&
                            !medication.medicationHistory.wasSkippedToday()
                        ) {
                            Log.d(TAG, "${medication.name} was not taken/skipped for scheduled time, marking as missed")
                            medication.markAsMissed(dateTime = LocalDateTime.now())
                            medication.id?.let {
                                FireStoreRepository.updateMedicationHistory(
                                    userId = userId,
                                    medicationId = it,
                                    event = MedicationEvent.Missed(date = LocalDateTime.now())
                                )
                            }
                        }

                        // Only update if we actually have dates that need updating
                        if (missedDueDates.isNotEmpty()) {
                            // Update each date individually - if it's in the past, add 7 days
                            val updatedDates = schedule.nextDueDates.map { dueDate ->
                                if (dueDate.toLocalDate() <= today) {
                                    dueDate.plusDays(7) // Add one week for weekly schedules
                                } else {
                                    dueDate // Keep future dates as is
                                }
                            }.sorted() // Sort to maintain chronological order

                            medication.id?.let { medId ->
                                val success = FireStoreRepository.updateMedicationDates(
                                    userId = userId,
                                    medicationId = medId,
                                    newDates = updatedDates
                                )

                                if (success) {
                                    // Schedule notification for the next upcoming date
                                    val nextDueTimeMillis = updatedDates[0]
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli()
                                    notificationHelper.scheduleNotification(
                                        medication.name,
                                        medication.dosage,
                                        nextDueTimeMillis
                                    )
                                    Log.d(TAG, "Successfully advanced weekly schedule for medication: ${medication.name}")
                                } else {
                                    Log.e(TAG, "Failed to advance weekly schedule for medication: ${medication.name}")
                                }
                            }
                        } else {
                            // If no dates needed updating, just schedule the next notification
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
                    }
                    else -> {
                        Log.d(TAG, "Schedule type not handled for medication: ${medication.name}")
                    }
                }
            }

            // Schedule the next midnight update
            scheduleNextMidnightWork(applicationContext)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in midnight worker: ${e.message}")
            Result.retry()
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