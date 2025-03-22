package com.mints.mobilehealthapplication.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import com.mints.mobilehealthapplication.data.NotificationHelper.Companion.NOTIFICATION_ACTION
import com.mints.mobilehealthapplication.data.NotificationHelper.Companion.SNOOZE_ACTION
import java.time.Instant
import java.util.Date

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotifDebug", "FULL INTENT DUMP START ==================")

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

        when(intent.action) {
            SNOOZE_ACTION -> {
                //Handle the snooze action
                val medicationName = intent.getStringExtra("medication_name")
                if (medicationName.isNullOrEmpty()) {
                    Log.e("NotifDebug","Medication name is null or empty for the snooze action.")
                    return
                }
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val snoozeDuration = prefs.getString("snooze_duration", "5")?.toLong() ?: 5L
                Log.d("NotifDebug","Snoozing alarm by: $snoozeDuration.")
                val snoozeDelayMillis = snoozeDuration * 60 * 1000L
                val newTime = System.currentTimeMillis() + snoozeDelayMillis
                val notificationHelper = NotificationHelper(context)
                notificationHelper.scheduleNotification(medicationName,newTime,true)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationId = medicationName.hashCode()
                notificationManager.cancel(notificationId)
                Log.d("NotifDebug","Snoozed $medicationName to ${Date(newTime)}")
            }
            NOTIFICATION_ACTION -> {
                //Handle a regular notification
                val medicationName = intent.getStringExtra("medication_name")
                val scheduleTime = intent.getLongExtra("schedule_time",0)
                Log.d("NotifDebug","Extracted medName: $medicationName")
                Log.d("NotifDebug","Extracted schedule time: ${Instant.ofEpochMilli(scheduleTime)}")
                if (medicationName.isNullOrEmpty()) {
                    Log.e("NotifDebug","Medication name is null or empty")
                    return
                }
                try {
                    val notificationHelper = NotificationHelper(context)
                    notificationHelper.showNotification(medicationName,scheduleTime)
                    Log.d("NotifDebug","Successfully showed notification for $medicationName")
                    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                    val backupEnabled = prefs.getBoolean("enabled_backup_reminder",true)
                    val isABackupNotification =  intent.getBooleanExtra("notification_is_backup", false)

                    Log.d("NotifDebug","Is backups enabled: $backupEnabled")
                    //Only schedule a backup notification if the notification received was not a backup notification
                    Log.d("NotifDebug","Is a backup notification: $isABackupNotification")
                    if(backupEnabled && !isABackupNotification) {
                        val backupDelayMinutes = prefs.getString("backup_reminder_delay","30")?.toLong()?:30L
                        val backupDelayMillis = backupDelayMinutes * 60 * 1000L
                        val backupTime = System.currentTimeMillis() + backupDelayMillis

                        //Schedule a backup notification for this medication
                        notificationHelper.scheduleNotification(medicationName,backupTime,true)
                        Log.d("NotifDebug", "Backup notification scheduled for $medicationName for ${Instant.ofEpochMilli(backupTime)}")


                    }

                }catch(e:Exception) {
                    Log.e("NotifDebug","Failed to show notification",e)
                }
            }
            else -> {
                Log.w("NotifDebug","Unexpected intent action ${intent.action}")
            }
        }
        Log.d("NotifDebug", "FULL INTENT DUMP END ==================")

    }
}