package com.mints.mobilehealthapplication.data

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.ui.MainActivity

class NotificationHelper(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "medication_reminder_channel"
        private const val CHANNEL_NAME = "Medication Reminders"
        const val NOTIFICATION_ACTION = "MEDICATION_NOTIFICATION_ACTION"
        const val SNOOZE_ACTION = "MEDICATION_SNOOZE_ACTION"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        Log.d("NotifDebug", "NotificationHelper Constructor Details:")
        Log.d("NotifDebug", "Context Type: ${context.javaClass.name}")
        Log.d("NotifDebug", "Context Hash: ${context.hashCode()}")
        Log.d("NotifDebug", "Application Context Hash: ${context.applicationContext.hashCode()}")
        Log.d("NotifDebug", "Is Same Context: ${context === context.applicationContext}")
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Medication reminder notifications"
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }



    fun showNotification(medicationName: String,scheduleTime:Long) {
        Log.d("NotifDebug", "Building notification for $medicationName")

        //Intent to open the app when the notification is tapped

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        //Create snooze intent
        val snoozeIntent = Intent(context,NotificationReceiver::class.java).apply {
            action = SNOOZE_ACTION
            putExtra("medication_name",medicationName)
            putExtra("schedule_time",scheduleTime)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeIntent.hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_prescriptions_24px)
            .setContentTitle("Medication Reminder")
            .setContentText("Time to take $medicationName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.baseline_ic_snooze,"Snooze",snoozePendingIntent)
        Log.i("NotifDebug", "Notification displayed for $medicationName")
        val notificationId = medicationName.hashCode()
        notificationManager.notify(notificationId, builder.build())

    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(medicationName: String, timeInMillis: Long) {
        val uniqueUri = Uri.parse("mints://notification/$medicationName/$timeInMillis")
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NOTIFICATION_ACTION
            data = uniqueUri
            putExtra("medication_name", medicationName)
            putExtra("schedule_time", timeInMillis)
            putExtra("thread", Thread.currentThread().name)
            putExtra("context_hash", context.hashCode())
        }

        val requestCode = uniqueUri.hashCode()

        //Check if there is already an existing PendingIntent
        val existingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )

        if(existingIntent !=null) {
            alarmManager.cancel(existingIntent)
        }
        //Create a new PendingIntent
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
            Log.d("NotifDebug", "Alarm set successfully")
        } catch (e: Exception) {
            Log.e("NotifDebug", "Failed to set alarm", e)
        }
    }


    fun getContext(): Context = context

}

