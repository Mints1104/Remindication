package com.mints.mobilehealthapplication.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("BootReceiver", "BootReceiver onReceive triggered")

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootDebug", "Boot completed. Scheduling reschedule worker.")

            val workRequest = OneTimeWorkRequestBuilder<RescheduleWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
