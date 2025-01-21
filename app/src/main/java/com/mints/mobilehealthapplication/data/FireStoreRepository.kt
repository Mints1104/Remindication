package com.mints.mobilehealthapplication.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalTime

object FireStoreRepository {
    private val db by lazy { FirebaseFirestore.getInstance() }

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

    @Suppress("UNCHECKED_CAST")
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
                    refillReminder = parseRefillInfo(document.get("refillReminder") as? Map<String, Any>)
                )
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

    private fun convertScheduleToMap(schedule: MedicationSchedule): Map<String, Any> {
        return when (schedule) {
            is MedicationSchedule.Daily -> hashMapOf(
                "type" to "daily",
                "frequency" to (schedule.frequency.ordinal + 1),
                "times" to schedule.times.map { it.toString() },
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

    @Suppress("UNCHECKED_CAST")
    private fun parseMedicationSchedule(scheduleMap: Map<String, Any>?): MedicationSchedule {
        if (scheduleMap == null) return MedicationSchedule.OnDemand()

        return when (scheduleMap["type"] as? String) {
            "daily" -> {
                val times = (scheduleMap["times"] as? List<*>)?.mapNotNull {
                    (it as? String)?.let { timeStr -> LocalTime.parse(timeStr) }
                } ?: emptyList()

                MedicationSchedule.Daily(
                    frequency = DailyFrequency.fromInt((scheduleMap["frequency"] as? Long)?.toInt() ?: 1),
                    times = times,
                    withFood = scheduleMap["withFood"] as? Boolean ?: false,
                    specificInstructions = scheduleMap["specificInstructions"] as? String ?: ""
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
                    withFood = scheduleMap["withFood"] as? Boolean ?: false
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