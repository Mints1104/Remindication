package com.mints.mobilehealthapplication.data

import com.google.firebase.Timestamp
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Medication(
    var id: String? = null,
    val name: String = "",
    val dosage: String = "",
    val schedule: MedicationSchedule,
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val active: Boolean = true,
    val lastModified: Timestamp = Timestamp.now(),
    val refillReminder: RefillInfo? = null,
)

sealed class MedicationSchedule {
    data class Daily(
        val frequency: DailyFrequency,
        val times: List<LocalTime>,
        val withFood: Boolean = false,
        val specificInstructions: String = "",
        val nextDueDates: List<LocalDateTime>


    ) : MedicationSchedule()

    data class Interval(
        val interval: IntervalPeriod,
        val startTime: LocalTime,
        val endDate: Timestamp? = null,
      //  val nextDueDates: List<LocalDateTime>

    ) : MedicationSchedule()

    data class WeeklySchedule(
        val days: List<DayOfWeek>,
        val times: List<LocalTime>,
        val withFood: Boolean = false,
        val nextDueDates: List<LocalDateTime>

    ) : MedicationSchedule()

    data class Cyclic(
        val intakeDays: Int,
        val pauseDays: Int,
        val times: List<LocalTime>,
   //     val nextDueDates: List<LocalDateTime>,
        val currentCycleStartDate: Timestamp? = null,

    ) : MedicationSchedule()

    data class OnDemand(
        val maxDailyDoses: Int? = null,
        val minTimeBetweenDoses: Int? = null,
        val instructions: String = ""
    ) : MedicationSchedule()

    // Formatting properties
    val formattedFrequency: String
        get() = when (this) {
            is Daily -> when (frequency) {
                DailyFrequency.ONCE -> "Once Daily"
                DailyFrequency.TWICE -> "Twice Daily"
            }
            is WeeklySchedule -> days.joinToString { it.shortName }
            is Cyclic -> "Cyclic ($intakeDays days on, $pauseDays days off)"
            is OnDemand -> "As Needed"
            is Interval -> "Every ${interval.value} ${interval.unit.name.lowercase()}"
        }
    val frequencyType: String
        get() = when(this) {

            is WeeklySchedule -> "Weekly"
            is Cyclic -> "Cyclic"
            is OnDemand -> "On demand"
            is Interval -> "Interval"
            is Daily -> when (frequency) {
                DailyFrequency.ONCE -> "Once Daily"
                DailyFrequency.TWICE -> "Twice Daily"
            }
        }



    val formattedTimes: String
        get() = when (this) {
            is Daily -> times.formatTimes()
            is WeeklySchedule -> times.formatTimes()
            is Cyclic -> times.formatTimes()
            is Interval -> startTime.formatTime()
            is OnDemand -> maxDailyDoses?.let { "Max $it doses/day" } ?: ""
        }

    val formattedDetails: String
        get() = when (this) {
            is Daily -> if (withFood) "With food" else specificInstructions
            is WeeklySchedule -> if (withFood) "With food" else ""
            is Cyclic -> currentCycleStartDate?.let { "Started ${it.toDate()}" } ?: ""
            is Interval -> endDate?.let { "Until ${it.toDate()}" } ?: ""
            is OnDemand -> minTimeBetweenDoses?.let { "Min ${it}h between" } ?: instructions
        }
}





// Extension functions
private fun List<LocalTime>.formatTimes(): String = joinToString(", ") { it.formatTime() }

private fun LocalTime.formatTime(): String =
    this.format(DateTimeFormatter.ofPattern("HH:mm"))

val DayOfWeek.shortName: String get() = when (this) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}

// Rest of your existing classes
data class RefillInfo(
    val pillsRemaining: Int,
    val totalPills: Int,
    val reminderThreshold: Int = 7
)

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
    private fun isValidDailySchedule(times: List<LocalTime>): Boolean {
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