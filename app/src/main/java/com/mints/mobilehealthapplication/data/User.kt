package com.mints.mobilehealthapplication.data

import com.google.firebase.Timestamp
import java.time.DayOfWeek
import java.time.LocalTime

data class Medication(
    var id: String? = null,
    val name: String = "",
    val dosage: String = "",
    val schedule: MedicationSchedule,
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val active: Boolean = true,  // To track if medication is currently active
    val lastModified: Timestamp = Timestamp.now(),
    val refillReminder: RefillInfo? = null  // Optional refill information
)

data class RefillInfo(
    val pillsRemaining: Int,
    val totalPills: Int,
    val reminderThreshold: Int = 7  // Remind when X days of pills remaining
)

sealed class MedicationSchedule {
    data class Daily(
        val frequency: DailyFrequency,
        val times: List<LocalTime>,
        val withFood: Boolean = false,  // Indicate if medication should be taken with food
        val specificInstructions: String = ""  // Any special instructions
    ) : MedicationSchedule()

    data class Interval(
        val interval: IntervalPeriod,
        val startTime: LocalTime,
        val endDate: Timestamp? = null  // Optional end date for temporary medications
    ) : MedicationSchedule()

    data class WeeklySchedule(
        val days: List<DayOfWeek>,
        val times: List<LocalTime>,
        val withFood: Boolean = false
    ) : MedicationSchedule()

    data class Cyclic(
        val intakeDays: Int,
        val pauseDays: Int,
        val times: List<LocalTime>,
        val currentCycleStartDate: Timestamp? = null  // Track current cycle
    ) : MedicationSchedule()

    data class OnDemand(
        val maxDailyDoses: Int? = null,  // Maximum allowed doses per day
        val minTimeBetweenDoses: Int? = null,  // Minimum hours between doses
        val instructions: String = ""
    ) : MedicationSchedule()
}

enum class DailyFrequency {
    ONCE,
    TWICE;

    companion object {
        fun fromInt(times: Int): DailyFrequency {
            return when (times) {
                1 -> ONCE
                2 -> TWICE
                else -> ONCE
            }
        }
    }
}

data class IntervalPeriod(
    val value: Int,
    val unit: IntervalUnit
) {
    init {
        require(value > 0) { "Interval value must be positive" }
    }
}

enum class IntervalUnit {
    HOURS,
    DAYS,
    WEEKS,
    MONTHS
}

object ScheduleValidator {
    fun isValidDailySchedule(times: List<LocalTime>): Boolean {
        if (times.isEmpty()) return false

        val sortedTimes = times.sorted()

        // For multiple daily doses, ensure minimum 2 hours between doses
        if (times.size > 1) {
            for (i in 0 until sortedTimes.size - 1) {
                val hoursBetween = java.time.Duration.between(
                    sortedTimes[i],
                    sortedTimes[i + 1]
                ).toHours()
                if (hoursBetween < 2) return false
            }
        }

        return true
    }

    fun isValidIntervalPeriod(interval: IntervalPeriod): Boolean {
        return when (interval.unit) {
            IntervalUnit.HOURS -> interval.value in 1..24
            IntervalUnit.DAYS -> interval.value in 1..31
            IntervalUnit.WEEKS -> interval.value in 1..52
            IntervalUnit.MONTHS -> interval.value in 1..12
        }
    }

    fun isValidCyclicSchedule(intakeDays: Int, pauseDays: Int, times: List<LocalTime>): Boolean {
        return intakeDays > 0 &&
                pauseDays >= 0 &&
                times.isNotEmpty() &&
                isValidDailySchedule(times)
    }

    fun isValidOnDemandSchedule(maxDailyDoses: Int?, minTimeBetweenDoses: Int?): Boolean {
        if (maxDailyDoses != null && maxDailyDoses <= 0) return false
        if (minTimeBetweenDoses != null && minTimeBetweenDoses <= 0) return false
        return true
    }
}