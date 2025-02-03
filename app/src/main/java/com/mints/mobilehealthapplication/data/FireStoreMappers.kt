package com.mints.mobilehealthapplication.data

import com.google.firebase.Timestamp
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class FireStoreMappers {



    fun MedicationEvent.toMap(): Map<String, Any> {
        return hashMapOf(
            "type" to when (this) {
                is MedicationEvent.Taken -> "taken"
                is MedicationEvent.Skipped -> "skipped"
                is MedicationEvent.Missed -> "missed"
            },
            "date" to Timestamp(this.date.toEpochSecond(ZoneOffset.UTC), 0),
        )
    }

    // Converts a LocalDateTime to a Firebase Timestamp.
    fun LocalDateTime.toFirebaseTimestamp(): Timestamp =
        Timestamp(java.util.Date.from(this.atZone(ZoneId.systemDefault()).toInstant()))

    // Converts a Firebase Timestamp to a LocalDateTime.
    fun Timestamp.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(this.toDate().toInstant(), ZoneId.systemDefault())

    // Extension function for formatting LocalTime.
    fun LocalTime.formatTime(): String =
        this.format(DateTimeFormatter.ofPattern("HH:mm"))

}