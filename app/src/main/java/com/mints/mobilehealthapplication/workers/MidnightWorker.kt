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
import com.google.firebase.Timestamp
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.ui.HomeFragment.Companion.REFRESH_ACTION
import com.mints.mobilehealthapplication.utils.ScheduleHelper
import com.mints.mobilehealthapplication.utils.toLocalDateTime
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
            val now = LocalDateTime.now()
            val userId = FireStoreRepository.getUser()?.uid ?: ""
            val medications = FireStoreRepository.getMedications(userId)
            val yesterday = LocalDate.now().minusDays(1)

            FireStoreRepository.updateAdherenceStreak(userId, yesterday)

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
                        Log.d(TAG, "Schedule type not handled for ${medication.name} of type ${medication.schedule.frequencyType}")
                    }
                }
            }
            scheduleNextMidnightWork(applicationContext)

            val refreshIntent = Intent(REFRESH_ACTION)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(refreshIntent)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in midnight worker: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun processCyclicSchedule(
        medication: Medication,
        schedule: MedicationSchedule.Cyclic,
        now: LocalDateTime,
        userId: String,
    ) {
        Log.d(TAG, "Processing cyclic schedule for ${medication.name}")

        // Handle missed doses
        val missedDueDates = schedule.nextDueDates.filter { it.isBefore(now) }
        if (missedDueDates.isNotEmpty()) {
            val missedEvents = mutableListOf<MedicationEvent>()
            val cycleLength = schedule.intakeDays + schedule.pauseDays

            // Use schedule's currentCycleStartDate to determine cycle position
            val cycleStartDate = schedule.currentCycleStartDate?.toLocalDateTime()?.toLocalDate()
                ?: missedDueDates.minByOrNull { it }?.toLocalDate()
                ?: now.toLocalDate()

            // Track dates we've already processed to avoid duplicates
            val processedDates = mutableSetOf<LocalDate>()

            missedDueDates.forEach { missedDateTime ->
                val startDate = missedDateTime.toLocalDate()
                val endDate = now.toLocalDate().minusDays(1)

                var currentDate = startDate
                while (!currentDate.isAfter(endDate)) {
                    if (currentDate in processedDates) {
                        currentDate = currentDate.plusDays(1)
                        continue
                    }

                    // Calculate day position in the cycle
                    val daysSinceCycleStart = java.time.temporal.ChronoUnit.DAYS.between(cycleStartDate, currentDate)
                    val dayInCycle = (daysSinceCycleStart % cycleLength).toInt()

                    // Only mark as missed if:
                    // 1. It's an intake day (day falls within intake period)
                    // 2. No event exists for this specific day
                    if (dayInCycle < schedule.intakeDays && !medication.medicationHistory.hadEventOnSpecificDay(currentDate)) {
                        val eventDateTime = currentDate.atTime(missedDateTime.toLocalTime())
                        Log.d(TAG, "Marking ${medication.name} as missed at $eventDateTime")

                        medication.markAsMissed(eventDateTime)
                        missedEvents.add(MedicationEvent.Missed(instant = eventDateTime.atZone(ZoneId.systemDefault()).toInstant()))
                        processedDates.add(currentDate)
                    }

                    currentDate = currentDate.plusDays(1)
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

        // Initial calculation
        var newDueDates: List<LocalDateTime>
        var newCycleStartDate: Timestamp

        val initialCalculation = ScheduleHelper.calculateCyclicDueDates(
            intakeDays = schedule.intakeDays,
            pauseDays = schedule.pauseDays,
            times = schedule.times,
            currentCycleStartDate = schedule.currentCycleStartDate
        )

        newDueDates = initialCalculation.first
        newCycleStartDate = initialCalculation.second

        // If all calculated dates are in the past, recalculate for next cycle
        if (newDueDates.all { it.isBefore(now) }) {
            Log.d(TAG, "All dates in the past, recalculating with new cycle start")
            val recalculation = ScheduleHelper.calculateCyclicDueDates(
                intakeDays = schedule.intakeDays,
                pauseDays = schedule.pauseDays,
                times = schedule.times,
                currentCycleStartDate = newCycleStartDate
            )
            newDueDates = recalculation.first
            newCycleStartDate = recalculation.second
        }

        medication.id?.let { medId ->
            // Update both the due dates and cycle start date
            val success = FireStoreRepository.updateCyclicMedication(
                userId = userId,
                medicationId = medId,
                newDates = newDueDates,
                newCycleStartDate = newCycleStartDate
            )

            if (success) {
                Log.d(TAG, "All new due dates: $newDueDates")

                // Filter out due dates that are not after the current time
                val upcomingDueDates = newDueDates.filter { it.isAfter(now) }
                Log.d(TAG, "Upcoming due dates after filtering: $upcomingDueDates")

                if (upcomingDueDates.isNotEmpty()) {
                    val nextDueDate = upcomingDueDates.minByOrNull { it }!!
                    val nextDueTimeMillis = nextDueDate
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()

                    Log.d(TAG, "Scheduling notification for ${medication.name} at $nextDueTimeMillis")
                    notificationHelper.scheduleNotification(medication.name, nextDueTimeMillis)
                    Log.d(TAG, "Successfully advanced cyclic schedule for ${medication.name}")
                } else {
                    Log.w(TAG, "No upcoming due dates for ${medication.name} even after recalculation!")
                }
            } else {
                Log.e(TAG, "Failed to update cyclic schedule for ${medication.name}")
            }
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
                val missedDates = ScheduleHelper.getDatesBetween(
                    start = missedDateTime.toLocalDate(),
                    end = now.toLocalDate().minusDays(1)
                )
                missedDates.forEach { date ->
                    val eventDateTime = date.atTime(missedDateTime.toLocalTime())
                    Log.d(TAG, "Marking ${medication.name} as missed at $eventDateTime")
                    medication.markAsMissed(eventDateTime)
                    missedEvents.add(
                        MedicationEvent.Missed(
                            instant = eventDateTime.atZone(ZoneId.systemDefault()).toInstant()
                        )
                    )
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
                val upcomingDates = updatedDates.filter { it.isAfter(now) || it.isEqual(now)}
                upcomingDates.forEach { dueDate ->
                    val dueTimeMillis = dueDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    Log.d(TAG, "Scheduling notification for ${medication.name} at $dueTimeMillis")
                    notificationHelper.scheduleNotification(medication.name, dueTimeMillis)
                }
//                val nextDueTimeMillis = updatedDates.minByOrNull { it }
//                    ?.atZone(ZoneId.systemDefault())
//                    ?.toInstant()
//                    ?.toEpochMilli() ?: 0L
//                Log.d(TAG, "Scheduling notification for ${medication.name} at $nextDueTimeMillis")
//                notificationHelper.scheduleNotification(medication.name, nextDueTimeMillis)
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
                    event = MedicationEvent.Missed(
                        instant = missedDateTime.atZone(ZoneId.systemDefault()).toInstant()
                    )
                )

            }
        }

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
