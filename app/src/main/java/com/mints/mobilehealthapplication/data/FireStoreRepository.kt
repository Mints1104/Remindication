package com.mints.mobilehealthapplication.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter



object FireStoreRepository {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val mappers = FireStoreMappers()

    suspend fun saveUserData(uid: String, data: RegistrationViewModel.RegistrationData): Boolean {
        return try {
            val userData = hashMapOf(
                "email" to data.email,
                "firstName" to data.firstName,
                "lastName" to data.lastName,
                "dateOfBirth" to data.dateOfBirth,
                "phoneNumber" to data.phoneNumber,
                "medication" to hashMapOf(
                    "name" to data.medicationName,
                    "dosage" to data.dosage,
                    "frequency" to data.frequency,
                    "reminderTime" to data.reminderTime
                ),
                "preferences" to hashMapOf(
                    "theme" to data.theme,
                    "enableNotifications" to data.enableNotifications
                ),
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("users").document(uid).set(userData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getMedications(uid: String): List<Medication> {
        return db.collection("users")
            .document(uid)
            .collection("medications")
            .get()
            .await()
            .map { document ->
                Medication(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    dosage = document.getString("dosage") ?: "",
                    schedule = parseMedicationSchedule(document.get("schedule") as? Map<String, Any>),
                    notes = document.getString("notes") ?: "",
                    createdAt = document.getTimestamp("createdAt") ?: Timestamp.now(),
                    active = document.getBoolean("active") ?: true,
                    lastModified = document.getTimestamp("lastModified") ?: Timestamp.now(),
                    refillReminder = parseRefillInfo(document.get("refillReminder") as? Map<String, Any>),
                    medicationHistory = parseMedicationHistory(document.get("medicationHistory") as? Map<String, Any>)

                )
            }
    }

    suspend fun getMedicationDetails(uid: String, medicationId: String): Medication {
        return try {
            val documentSnapshot = db.collection("users")
                .document(uid)
                .collection("medications")
                .document(medicationId)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                Medication(
                    id = documentSnapshot.id,
                    name = documentSnapshot.getString("name") ?: "",
                    dosage = documentSnapshot.getString("dosage") ?: "",
                    schedule = parseMedicationSchedule(documentSnapshot.get("schedule") as? Map<String, Any>),
                    notes = documentSnapshot.getString("notes") ?: "",
                    createdAt = documentSnapshot.getTimestamp("createdAt") ?: Timestamp.now(),
                    active = documentSnapshot.getBoolean("active") ?: true,
                    lastModified = documentSnapshot.getTimestamp("lastModified") ?: Timestamp.now(),
                    refillReminder = parseRefillInfo(documentSnapshot.get("refillReminder") as? Map<String, Any>)
                )
            } else {
                throw Exception("Medication not found!")
            }
        } catch (e: Exception) {
            throw Exception("Error fetching medication details: ${e.message}")
        }
    }

    suspend fun updateMedicationHistory(
        userId: String,
        medicationId: String,
        event: MedicationEvent
    ): Boolean {
        return try {
            val documentPath = "users/$userId/medications/$medicationId"
            Log.d("FireStoreRepo", "Updating medication history at path: [$documentPath] with event type: ${event::class.simpleName}")

            // Use the mapper's extension function:
            val eventMap = with(mappers) { event.toMap() }

            db.collection("users")
                .document(userId)
                .collection("medications")
                .document(medicationId)
                .update("medicationHistory.events", FieldValue.arrayUnion(eventMap))
                .await()

            Log.d("FireStoreRepo", "Successfully updated medication history at path: [$documentPath]")
            true
        } catch (e: FirebaseFirestoreException) {
            Log.e("FireStoreRepo", "Firestore update failed at path [$userId/$medicationId]: ${e.code} - ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e("FireStoreRepo", "Unexpected error updating medication history at path [$userId/$medicationId]: ${e.message}", e)
            false
        }
    }






    // Update the parsing method to handle potential null cases
    private fun parseMedicationHistory(historyMap: Map<String, Any>?): MedicationHistory {
        if (historyMap == null) return MedicationHistory()

        val events = (historyMap["events"] as? List<Map<String, Any>> ?: emptyList()).mapNotNull { eventMap ->
            val date = (eventMap["date"] as? Timestamp)?.let { timestamp ->
                LocalDateTime.ofInstant(timestamp.toDate().toInstant(), ZoneId.systemDefault())
            } ?: LocalDateTime.now()

            val notes = eventMap["notes"] as? String ?: ""
            val type = eventMap["type"] as? String

            when (type) {
                "taken" -> MedicationEvent.Taken(date, notes)
                "skipped" -> MedicationEvent.Skipped(date, notes)
                "missed" -> MedicationEvent.Missed(date, notes)
                else -> null
            }
        }

        return MedicationHistory(events.toMutableList())
    }





    suspend fun updateMedication(userId: String, medication: Medication): Boolean {
        return try {
            val medicationId = medication.id ?: run {
                Log.e("FireStoreRepo", "Update failed: medicationId is null for user: $userId")
                return false
            }

            Log.d("FireStoreRepo", "Updating medication [$medicationId] for user [$userId]")

            val medicationData = hashMapOf(
                "name" to medication.name,
                "dosage" to medication.dosage,
                "schedule" to convertScheduleToMap(medication.schedule),
                "active" to medication.active,
                "lastModified" to medication.lastModified,
                "notes" to medication.notes
            )

            Log.d("FireStoreRepo", "Medication data: ${medicationData.keys}")

            db.collection("users")
                .document(userId)
                .collection("medications")
                .document(medicationId)
                .update(medicationData)
                .await()

            Log.d("FireStoreRepo", "Successfully updated medication [$medicationId] for user [$userId]")
            true
        } catch (e: FirebaseFirestoreException) {
            Log.e("FireStoreRepo", "Firestore update failed for [$userId/${medication.id}]: ${e.code} - ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e("FireStoreRepo", "Unexpected error updating medication [$userId/${medication.id}]: ${e.javaClass.simpleName} - ${e.message}", e)
            false
        }
    }

    suspend fun saveMedication(uid: String, medication: Medication): Boolean {
        return try {
            val medicationData = hashMapOf(
                "name" to medication.name,
                "dosage" to medication.dosage,
                "schedule" to convertScheduleToMap(medication.schedule),
                "active" to medication.active,
                "lastModified" to medication.lastModified,
                "notes" to medication.notes,
                "createdAt" to FieldValue.serverTimestamp()
            )

            medication.refillReminder?.let { refillInfo ->
                medicationData["refillReminder"] = hashMapOf(
                    "pillsRemaining" to refillInfo.pillsRemaining,
                    "totalPills" to refillInfo.totalPills,
                    "reminderThreshold" to refillInfo.reminderThreshold
                )
            }

            val documentReference = db.collection("users")
                .document(uid)
                .collection("medications")
                .add(medicationData)
                .await()
            medication.id = documentReference.id
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteMedication(uid: String, medicationId: String) {
        val tag = "FireStoreRepository"
        try {
            Log.i(tag, "🔥 Initiating deletion for medication ID: $medicationId (User: ${uid.take(4)}...)")
            Log.d(tag, "🗄️ Database path: users/$uid/medications/$medicationId")

            val startTime = System.currentTimeMillis()
            db.collection("users").document(uid)
                .collection("medications")
                .document(medicationId)
                .delete()
                .await()

            val duration = System.currentTimeMillis() - startTime
            Log.i(tag, "✅ Successfully deleted medication ID: $medicationId in ${duration}ms")
        } catch (e: Exception) {
            Log.e(tag, "❌ FAILED to delete medication ID: $medicationId", e)
            Log.w(tag, "⚠️ Error details: ${e.message?.take(200)}...")
            Log.d(tag, "🔄 Possible mitigation: Verify network connection and document permissions")
            throw e
        }
    }


    private fun LocalDateTime.toFirestoreTimestamp(): Timestamp {
        return Timestamp(this.toEpochSecond(ZoneOffset.UTC), 0)
    }


    private fun convertScheduleToMap(schedule: MedicationSchedule): Map<String, Any> {
        return when (schedule) {
            is MedicationSchedule.Daily -> hashMapOf(
                "type" to "daily",
                "frequency" to (schedule.frequency.ordinal + 1),
                "times" to schedule.times.map { it.toString() },
                "nextDueDates" to schedule.nextDueDates.map { it.toFirestoreTimestamp() },
                "withFood" to schedule.withFood,
                "specificInstructions" to schedule.specificInstructions
            )

            is MedicationSchedule.Interval -> hashMapOf(
                "type" to "interval",
                "interval" to hashMapOf(
                    "value" to schedule.interval.value,
                    "unit" to schedule.interval.unit.name
                ),
                "startTime" to schedule.startTime.toString(),
                "endDate" to (schedule.endDate ?: Timestamp.now())
            )

            is MedicationSchedule.WeeklySchedule -> hashMapOf(
                "type" to "weekly",
                "days" to schedule.days.map { it.name },
                "times" to schedule.times.map { it.toString() },
                "nextDueDates" to schedule.nextDueDates.map { it.toFirestoreTimestamp() },
                "withFood" to schedule.withFood
            )

            is MedicationSchedule.Cyclic -> hashMapOf(
                "type" to "cyclic",
                "intakeDays" to schedule.intakeDays,
                "pauseDays" to schedule.pauseDays,
                "times" to schedule.times.map { it.toString() },
                "currentCycleStartDate" to (schedule.currentCycleStartDate ?: Timestamp.now())
            )

            is MedicationSchedule.OnDemand -> hashMapOf(
                "type" to "onDemand",
                "maxDailyDoses" to (schedule.maxDailyDoses ?: 0),
                "minTimeBetweenDoses" to (schedule.minTimeBetweenDoses ?: 0),
                "instructions" to schedule.instructions
            )
        }
    }

    private fun parseMedicationSchedule(scheduleMap: Map<String, Any>?): MedicationSchedule {
        if (scheduleMap == null) return MedicationSchedule.OnDemand()

        return when (scheduleMap["type"] as? String) {
            "daily" -> {
                val times = (scheduleMap["times"] as? List<*>)?.mapNotNull {
                    (it as? String)?.let { timeStr -> LocalTime.parse(timeStr) }
                } ?: emptyList()

                MedicationSchedule.Daily(
                    frequency = DailyFrequency.fromInt(
                        (scheduleMap["frequency"] as? Long)?.toInt() ?: 1
                    ),

                    times = times,
                    withFood = scheduleMap["withFood"] as? Boolean ?: false,
                    specificInstructions = scheduleMap["specificInstructions"] as? String ?: "",
                    nextDueDates = (scheduleMap["nextDueDates"] as? List<Timestamp>)?.map { timestamp ->
                        LocalDateTime.ofInstant(
                            timestamp.toDate().toInstant(),
                            ZoneId.systemDefault()
                        )
                    } ?: emptyList()
                )

            }

            "interval" -> {
                val intervalMap = scheduleMap["interval"] as? Map<String, Any>
                val interval = IntervalPeriod(
                    value = (intervalMap?.get("value") as? Long)?.toInt() ?: 1,
                    unit = IntervalUnit.valueOf((intervalMap?.get("unit") as? String) ?: IntervalUnit.DAYS.name)
                )

                MedicationSchedule.Interval(
                    interval = interval,
                    startTime = LocalTime.parse(scheduleMap["startTime"] as? String ?: "09:00"),
                    endDate = scheduleMap["endDate"] as? Timestamp
                )
            }

            "weekly" -> {
                val days = (scheduleMap["days"] as? List<*>)?.mapNotNull {
                    (it as? String)?.let { dayStr -> DayOfWeek.valueOf(dayStr) }
                } ?: emptyList()

                val times = (scheduleMap["times"] as? List<*>)?.mapNotNull {
                    (it as? String)?.let { timeStr -> LocalTime.parse(timeStr) }
                } ?: emptyList()

                MedicationSchedule.WeeklySchedule(
                    days = days,
                    times = times,
                    withFood = scheduleMap["withFood"] as? Boolean ?: false,
                    nextDueDates = (scheduleMap["nextDueDates"] as? List<Timestamp>)?.map { timestamp ->
                        LocalDateTime.ofInstant(
                            timestamp.toDate().toInstant(),
                            ZoneId.systemDefault()
                        )
                    } ?: emptyList()
                )
            }

            "cyclic" -> {
                val times = (scheduleMap["times"] as? List<*>)?.mapNotNull {
                    (it as? String)?.let { timeStr -> LocalTime.parse(timeStr) }
                } ?: emptyList()

                MedicationSchedule.Cyclic(
                    intakeDays = (scheduleMap["intakeDays"] as? Long)?.toInt() ?: 1,
                    pauseDays   = (scheduleMap["pauseDays"] as? Long)?.toInt() ?: 0,
                    times = times,
                    currentCycleStartDate = scheduleMap["currentCycleStartDate"] as? Timestamp
                )
            }

            "onDemand" -> {
                MedicationSchedule.OnDemand(
                    maxDailyDoses = (scheduleMap["maxDailyDoses"] as? Long)?.toInt(),
                    minTimeBetweenDoses = (scheduleMap["minTimeBetweenDoses"] as? Long)?.toInt(),
                    instructions = scheduleMap["instructions"] as? String ?: ""
                )
            }

            else -> MedicationSchedule.OnDemand()
        }
    }



    suspend fun updateMedicationDates(
        userId: String,
        medicationId: String,
        newDates: List<LocalDateTime>
    ): Boolean {
        return try {
            // Use our mapper's extension function for each date.
            val firestoreDates = newDates.map { with(mappers) { it.toFirebaseTimestamp() } }

            db.collection("users")
                .document(userId)
                .collection("medications")
                .document(medicationId)
                .update("schedule.nextDueDates", firestoreDates)
                .await()

            newDates.forEach { date ->
                Log.d("FIRESTORE_UPDATE", "Success, new date: $date")
            }

            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_UPDATE", "Failed to update dates", e)
            false
        }
    }




    private fun parseRefillInfo(refillMap: Map<String, Any>?): RefillInfo? {
        if (refillMap == null) return null

        return RefillInfo(
            pillsRemaining = (refillMap["pillsRemaining"] as? Long)?.toInt() ?: 0,
            totalPills = (refillMap["totalPills"] as? Long)?.toInt() ?: 0,
            reminderThreshold = (refillMap["reminderThreshold"] as? Long)?.toInt() ?: 7
        )
    }

    fun retrieveUserInfo(userId: String, onSuccess: (DocumentSnapshot) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }
}