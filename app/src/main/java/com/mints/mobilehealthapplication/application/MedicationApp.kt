package com.mints.mobilehealthapplication.application

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.workers.MidnightWorkerFactory

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
}
