package com.mints.mobilehealthapplication.data

import com.google.firebase.Timestamp

data class UserData(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val dateOfBirth: String = "",
    val phoneNumber: String = "",
    val emergencyContact: EmergencyContact = EmergencyContact(),
    val timezone: String = "",
    val healthInfo: HealthInfo = HealthInfo()

)

data class UserMedication(
    val medicationList: List<Medication>
)

data class Medication(
    var id: String? = null,
    val name: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val time: String = "",
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class EmergencyContact(
    val name: String = "",
    val relationship: String = "",
    val phoneNumber: String = ""
)


data class HealthInfo(
    val allergies: List<String> = listOf(),
    val conditions: List<String> = listOf(),
    val notes: String = ""
)