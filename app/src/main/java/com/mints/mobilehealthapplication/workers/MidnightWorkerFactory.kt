package com.mints.mobilehealthapplication.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.mints.mobilehealthapplication.data.NotificationHelper

class MidnightWorkerFactory(
    private val notificationHelper: NotificationHelper
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            MidnightWorker::class.java.name ->
                MidnightWorker(appContext, workerParameters, notificationHelper)
            RescheduleWorker::class.java.name ->
                RescheduleWorker(appContext, workerParameters, notificationHelper)
            else -> null
        }
    }
}