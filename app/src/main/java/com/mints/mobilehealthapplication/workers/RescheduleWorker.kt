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

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Running RescheduleWorker after reboot")
            // Ensure MidnightWorker scheduling is set up.
            MidnightWorker.initialize(applicationContext)
            val userId = FireStoreRepository.getUser()?.uid ?: ""
            val medications = FireStoreRepository.getMedications(userId)
            val notificationHelper = NotificationHelper(applicationContext)
            val now = LocalDateTime.now()

            medications.forEach { medication ->
                when (val schedule = medication.schedule) {
                    is MedicationSchedule.Daily -> {
                        // Process past due dates (mark as missed)
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
                        // Adjust future due dates using ScheduleHelper.
                        val updatedDates = schedule.nextDueDates.map { dueDate ->
                            if (dueDate.isBefore(now)) ScheduleHelper.adjustDailyDueDate(dueDate, now)
                            else dueDate
                        }.distinct()
                        medication.id?.let { medId ->
                            FireStoreRepository.updateMedicationDates(userId, medId, updatedDates)
                        }
                        // Schedule notifications for upcoming dates.
                        schedule.nextDueDates.forEach { dueDate ->
                            val dueMillis = dueDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            if (dueMillis > System.currentTimeMillis()) {
                                notificationHelper.scheduleNotification(medication.name, dueMillis)
                                Log.d(TAG, "Rescheduled notification for ${medication.name} at $dueMillis")
                            }
                        }
                    }
                    is MedicationSchedule.WeeklySchedule -> {
                        // Process past due dates (mark as missed)
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
                        // Adjust future due dates using ScheduleHelper.
                        val updatedDates = schedule.nextDueDates.map { dueDate ->
                            if (dueDate.isBefore(now)) ScheduleHelper.adjustWeeklyDueDate(dueDate, now)
                            else dueDate
                        }.sorted()
                        medication.id?.let { medId ->
                            FireStoreRepository.updateMedicationDates(userId, medId, updatedDates)
                        }
                        // Schedule notifications for upcoming dates.
                        schedule.nextDueDates.forEach { dueDate ->
                            val dueMillis = dueDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            if (dueMillis > System.currentTimeMillis()) {
                                notificationHelper.scheduleNotification(medication.name, dueMillis)
                                Log.d(TAG, "Rescheduled notification for ${medication.name} at $dueMillis")
                            }
                        }
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

    companion object {
        private const val TAG = "RescheduleWorker"
    }
}
