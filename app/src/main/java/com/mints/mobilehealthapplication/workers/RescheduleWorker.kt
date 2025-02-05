package com.mints.mobilehealthapplication.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.NotificationHelper

class RescheduleWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Running RescheduleWorker after reboot")

            // Initialize MidnightWorker to ensure the midnight schedule continues
            MidnightWorker.initialize(applicationContext)

            // Fetch medications from Firestore and reschedule their notifications
            val userId = FireStoreRepository.getUser()?.uid ?: ""
            val medications = FireStoreRepository.getMedications(userId)

            val notificationHelper = NotificationHelper(applicationContext)

            medications.forEach { medication ->

                if (medication.schedule is MedicationSchedule.Daily) {
                    medication.schedule.nextDueDates.forEach { dueDate ->
                        val dueMillis = dueDate.atZone(java.time.ZoneId.systemDefault())
                            .toInstant().toEpochMilli()

                        // Schedule a notification if the due time is in the future
                        if (dueMillis > System.currentTimeMillis()) {
                            notificationHelper.scheduleNotification(
                                medication.name,
                                dueMillis
                            )
                            Log.d(
                                TAG,
                                "Rescheduled notification for ${medication.name} at $dueMillis"
                            )
                        }
                    }
                }

                if (medication.schedule is MedicationSchedule.WeeklySchedule) {
                    medication.schedule.nextDueDates.forEach { dueDate ->
                        val dueMillis = dueDate.atZone(java.time.ZoneId.systemDefault())
                            .toInstant().toEpochMilli()

                        // Schedule a notification if the due time is in the future
                        if (dueMillis > System.currentTimeMillis()) {
                            notificationHelper.scheduleNotification(
                                medication.name,
                                dueMillis
                            )
                            Log.d(
                                TAG,
                                "Rescheduled notification for ${medication.name} at $dueMillis"
                            )
                        }
                    }
                }

            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in RescheduleWorker: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RescheduleWorker"
    }
}
