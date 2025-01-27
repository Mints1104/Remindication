package com.mints.mobilehealthapplication.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "Received broadcast for notification")

        if (intent.action == NotificationHelper.NOTIFICATION_ACTION) {
            val medicationName = intent.getStringExtra("medication_name")
            val dosage = intent.getStringExtra("dosage")

            Log.d("NotificationReceiver", "Medication: $medicationName, Dosage: $dosage")

            val notificationHelper = NotificationHelper(context)
            notificationHelper.showNotification(medicationName ?: "Unknown", dosage ?: "Unknown")
        } else {
            Log.w("NotifDebug", "Received unexpected intent action: ${intent.action}")

        }
    }
}