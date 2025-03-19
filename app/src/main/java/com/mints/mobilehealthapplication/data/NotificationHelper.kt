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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    fun cancelBackupNotification(medicationName: String) {
        val backupUri = Uri.parse("mints://notification/backup/$medicationName")
        val requestCode = backupUri.hashCode()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NOTIFICATION_ACTION
            data = backupUri
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )

        if(pendingIntent !=null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            Log.d("NotifDebug", "Cancelled backup notification for $medicationName")

        } else {
            Log.d("NotifDebug","Failed to cancel backup notification")
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(medicationName.hashCode())
    }

    fun showCustomNotification(title:String,message:String) {

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_prescriptions_24px)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(title.hashCode(), builder.build())
        notificationManager.notify(message.hashCode(), builder.build())


    }

    fun cancelRegularNotification(medicationName: String, timeInMillis: Long) {
        // Construct the same unique URI used for regular notifications.
        val uniqueUri = Uri.parse("mints://notification/$medicationName/$timeInMillis")
        val requestCode = uniqueUri.hashCode()

        // Create an intent with the same action and data.
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NOTIFICATION_ACTION
            data = uniqueUri
        }

        // Get the existing PendingIntent, if any.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )

        // If there's an existing intent, cancel it using the AlarmManager.
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            Log.d("NotifDebug", "Cancelled regular notification for $medicationName")
        } else {
            Log.d("NotifDebug", "No regular notification found to cancel for $medicationName")
        }

        // Also cancel the notification from the NotificationManager.
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(requestCode)
    }



    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(medicationName: String, timeInMillis:Long, isBackup:Boolean = false) {
        val uriPath = if(isBackup) "backup" else medicationName
        val uniqueUri = if(isBackup) {
            Uri.parse("mints://notification/$uriPath/$medicationName")
        } else {
            Uri.parse("mints://notification/$uriPath/$timeInMillis")
        }
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NOTIFICATION_ACTION
            data = uniqueUri
            putExtra("medication_name", medicationName)
            putExtra("schedule_time", timeInMillis)
            putExtra("thread", Thread.currentThread().name)
            putExtra("context_hash", context.hashCode())
        }

        val requestCode = uniqueUri.hashCode()

        val existingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )

        if(existingIntent !=null) {
            alarmManager.cancel(existingIntent)
        }
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
            val instant = Instant.ofEpochMilli(timeInMillis)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
            val formattedDate = formatter.format(instant)

            Log.d("NotifDebug", "Alarm set successfully at: $formattedDate for $medicationName")
        } catch (e: Exception) {
            Log.e("NotifDebug", "Failed to set alarm", e)
        }
    }


    fun getContext(): Context = context

}

