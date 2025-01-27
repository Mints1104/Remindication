package com.mints.mobilehealthapplication.data

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.ui.MainActivity
import java.util.Date

class NotificationHelper(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "medication_reminder_channel"
        private const val CHANNEL_NAME = "Medication Reminders"
        const val NOTIFICATION_ACTION = "MEDICATION_NOTIFICATION_ACTION"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    fun showNotification(medicationName: String, dosage: String) {
        Log.d("NotifDebug", "Building notification for $medicationName")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_prescriptions_24px)
            .setContentTitle("Medication Reminder")
            .setContentText("Time to take $medicationName - $dosage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        Log.i("NotifDebug", "Notification displayed for $medicationName")

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(medicationName: String, dosage: String, timeInMillis: Long) {
        Log.d("NotifDebug", "Scheduling notification for $medicationName at ${Date(timeInMillis)}")

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NOTIFICATION_ACTION
            putExtra("medication_name", medicationName)
            putExtra("dosage", dosage)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            timeInMillis.toInt(), // Use time as request code for unique PendingIntents
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }
}

