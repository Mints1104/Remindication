package com.mints.mobilehealthapplication.application

import android.app.Application
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.workers.MidnightWorkerFactory
import com.mints.mobilehealthapplication.workers.RescheduleWorker

class MedicationApp : Application() {
    val notificationHelper by lazy { NotificationHelper(applicationContext) }

    override fun onCreate() {
        super.onCreate()

        val factory = MidnightWorkerFactory(notificationHelper)

        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(factory)
                .build()
        )

    }

    fun scheduleRescheduleWorker() {
        val workRequest = OneTimeWorkRequestBuilder<RescheduleWorker>().build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    companion object {
        // Static access for use from anywhere
        fun scheduleRescheduleWorker(context: android.content.Context) {
            val workRequest = OneTimeWorkRequestBuilder<RescheduleWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
