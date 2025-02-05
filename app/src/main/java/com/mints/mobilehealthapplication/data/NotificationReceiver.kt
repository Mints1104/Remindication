package com.mints.mobilehealthapplication.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mints.mobilehealthapplication.data.NotificationHelper.Companion.NOTIFICATION_ACTION
import java.util.Date

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotifDebug", "FULL INTENT DUMP START ==================")

        // Log ALL extras
        intent.extras?.keySet()?.forEach { key ->
            Log.d("NotifDebug", "Extra Key: $key, Value: ${intent.extras?.get(key)}")
        }

        Log.d("NotifDebug", """
        Detailed Notification Received:
        Action: ${intent.action}
        Medication: ${intent.getStringExtra("medication_name")}
        Schedule time (getLongExtra): ${Date(intent.getLongExtra("schedule_time", 0))}
        Current time: ${Date(System.currentTimeMillis())}
    """.trimIndent())
        var action = ""
        if(intent.action.isNullOrEmpty()) {
            action = NOTIFICATION_ACTION
        }

        // Explicitly check for the NOTIFICATION_ACTION
        if (intent.action == NOTIFICATION_ACTION || action == NOTIFICATION_ACTION) {
            val medicationName = intent.getStringExtra("medication_name")
            val scheduleTime = intent.getLongExtra("schedule_time", 0)

            Log.d("NotifDebug", "Extracted medication name: $medicationName")
            Log.d("NotifDebug", "Extracted schedule time: ${Date(scheduleTime)}")

            if (medicationName == null) {
                Log.e("NotifDebug", "Received null medication name!")
                return
            }

            try {
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showNotification(medicationName)
                Log.d("NotifDebug", "Successfully showed notification for $medicationName")
            } catch (e: Exception) {
                Log.e("NotifDebug", "Failed to show notification", e)
            }
        } else {
            Log.w("NotifDebug", "Unexpected intent action: ${intent.action}")
        }

        Log.d("NotifDebug", "FULL INTENT DUMP END ==================")
    }
}