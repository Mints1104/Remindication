package com.mints.mobilehealthapplication.data

data class UserBadges(
    val currentStreak: Int = 0,
    val medicationCount: Int = 0,
    val perfectWeeks: Int = 0,
    val tookFirstMedication: Boolean = false,
    val totalDosesTaken: Int = 0
)