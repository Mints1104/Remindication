package com.mints.mobilehealthapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mints.mobilehealthapplication.data.NotificationHelper

class HomeFragmentViewModelFactory(
    private val notificationHelper: NotificationHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeFragmentViewModel(notificationHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}