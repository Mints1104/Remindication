package com.mints.mobilehealthapplication.workers

// MidnightWorker.kt
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class MidnightWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            // Debug logging to verify execution time
            val currentTime = LocalDateTime.now()
            Log.d("MidnightWorker", "Worker executed at: $currentTime")

            // Schedule the next run
            scheduleNextMidnightWork(applicationContext)

            return Result.success()
        } catch (e: Exception) {
            Log.e("MidnightWorker", "Error in midnight worker: ${e.message}")
            return Result.retry()
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
          //  val nextMidnight = now.plusMinutes(1)

            val delayInMillis = nextMidnight
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli() - System.currentTimeMillis()

            Log.d(TAG, "Scheduling next midnight work. Current time: $now")
            Log.d(TAG, "Next scheduled time: $nextMidnight")
            Log.d(TAG, "Delay in milliseconds: $delayInMillis")

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

        fun cancelWork(context: Context) {
            Log.d(TAG, "Cancelling midnight work")
            WorkManager.getInstance(context).cancelUniqueWork(MIDNIGHT_WORK_NAME)
        }
    }
}