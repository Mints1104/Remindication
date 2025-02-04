package com.mints.mobilehealthapplication.viewmodels
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mints.mobilehealthapplication.data.NotificationHelper

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize NotificationHelper using the application context.
    private val notificationHelper = NotificationHelper(application)

    fun scheduleMedicationNotification(medicationName: String, triggerTimeInMillis: Long) {
        notificationHelper.scheduleNotification(medicationName, triggerTimeInMillis)
    }
}
