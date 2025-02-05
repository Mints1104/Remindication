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
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class   MidnightWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {



    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "LETS GOOOO ITS MIDNIIIIGHTTTTTTT")
            val now = LocalDateTime.now()
            val userId = FireStoreRepository.getUser()?.uid ?: ""
            val medications = FireStoreRepository.getMedications(userId)

            medications.forEach { medication ->
                when (val schedule = medication.schedule) {
                    is MedicationSchedule.Daily -> {
                        Log.d(TAG, "Original date for ${medication.name}: ${schedule.nextDueDates}")
                        val missedDueDates = schedule.nextDueDates.filter {
                            it.isBefore(now)  // Compare with current time instead of just the date
                        }

                        if (missedDueDates.isNotEmpty() &&
                            !medication.medicationHistory.wasTakenToday() &&
                            !medication.medicationHistory.wasSkippedToday()
                        ) {
                            // Use the earliest missed due date as the missed time
                            val missedDateTime = missedDueDates.minOf { it }
                            Log.d(TAG, "${medication.name} was not taken/skipped for time: $missedDateTime, marking as missed")
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
                                val nextDueTimeMillis = updatedDates[0]
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                                Log.d(TAG,"Attempting to schedule noti for ${medication.name} at time: $nextDueTimeMillis")
                                notificationHelper.scheduleNotification(
                                    medication.name,
                                    nextDueTimeMillis,

                                )
                                Log.d(TAG, "Successfully advanced schedule for medication: ${medication.name}")
                            } else {
                                Log.e(TAG, "Failed to advance schedule for medication: ${medication.name}")
                            }
                        }
                    }
                    is MedicationSchedule.WeeklySchedule -> {
                        Log.d(TAG, "Original weekly dates for ${medication.name}: ${schedule.nextDueDates}")

                        val missedDueDates = schedule.nextDueDates.filter {
                            it.isBefore(now)
                        }

                        if (missedDueDates.isNotEmpty() &&
                            !medication.medicationHistory.wasTakenToday() &&
                            !medication.medicationHistory.wasSkippedToday()
                        ) {
                            // Use the earliest missed due date as the missed time
                            val missedDateTime = missedDueDates.minOf { it }
                            Log.d(TAG, "${medication.name} was not taken/skipped for time: $missedDateTime, marking as missed")
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
                                    val nextDueTimeMillis = updatedDates[0]
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli()
                                    notificationHelper.scheduleNotification(
                                        medication.name,
                                        nextDueTimeMillis
                                    )
                                    Log.d(TAG, "Successfully advanced weekly schedule for medication: ${medication.name}")
                                } else {
                                    Log.e(TAG, "Failed to advance weekly schedule for medication: ${medication.name}")
                                }
                            }
                        } else {
                            val nextDueTimeMillis = schedule.nextDueDates[0]
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            notificationHelper.scheduleNotification(
                                medication.name,
                                nextDueTimeMillis
                            )
                        }
                    }
                    else -> {
                        Log.d(TAG, "Schedule type not handled for medication: ${medication.name}")
                    }
                }
            }

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

            // Create NotificationHelper with application context
            NotificationHelper(context.applicationContext)

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