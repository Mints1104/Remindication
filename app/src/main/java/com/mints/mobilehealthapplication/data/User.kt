package com.mints.mobilehealthapplication.data

import com.google.firebase.Timestamp
import java.time.DayOfWeek
import java.time.LocalDate
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
    val medicationHistory: MedicationHistory = MedicationHistory()
) {
    fun markAsTaken(dateTime: LocalDateTime = LocalDateTime.now()) {
        medicationHistory.addEvent(MedicationEvent.Taken(date = dateTime))
    }

    fun markAsSkipped(dateTime: LocalDateTime = LocalDateTime.now()) {
        medicationHistory.addEvent(MedicationEvent.Skipped(date = dateTime))
    }

    fun markAsMissed(dateTime: LocalDateTime = LocalDateTime.now()) {
        medicationHistory.addEvent(MedicationEvent.Missed(date = dateTime))
    }


}


sealed class MedicationEvent {
    abstract val date: LocalDateTime
    abstract val type: EventType

    enum class EventType {
        TAKEN, SKIPPED, MISSED
    }

    data class Taken(
        override val date: LocalDateTime = LocalDateTime.now(),
    ) : MedicationEvent() {
        override val type = EventType.TAKEN
    }

    data class Skipped(
        override val date: LocalDateTime = LocalDateTime.now(),
    ) : MedicationEvent() {
        override val type = EventType.SKIPPED
    }

    data class Missed(
        override val date: LocalDateTime = LocalDateTime.now(),
    ) : MedicationEvent() {
        override val type = EventType.MISSED
    }
}

data class MedicationHistory(
    val events: MutableList<MedicationEvent> = mutableListOf()
) {
    // Add a new event to the history
    fun addEvent(event: MedicationEvent) {
        events.add(event)
        // Sort events by date to ensure proper ordering
        events.sortByDescending { it.date }
    }

    fun getAllEvents(): MutableList<MedicationEvent> {
        return events
    }

    // Get all events for a specific status using the new EventType
    fun getEventsByType(type: MedicationEvent.EventType): List<MedicationEvent> {
        return events.filter { it.type == type }
    }

    // Get events within a specific date range
    fun getEventsInDateRange(start: LocalDateTime, end: LocalDateTime): List<MedicationEvent> {
        return events.filter {
            (it.date.isEqual(start) || it.date.isAfter(start)) &&
                    (it.date.isEqual(end) || it.date.isBefore(end))
        }
    }

    // Get the latest event if available
    fun getLastEvent(): MedicationEvent? {
        return events.firstOrNull() // Since we keep the list sorted, first is most recent
    }

    // Get the last event of a specific type
    fun getLastEventOfType(type: MedicationEvent.EventType): MedicationEvent? {
        return events.firstOrNull { it.type == type }
    }

    // Get events from the last n days
    fun getEventsFromLastDays(days: Int): List<MedicationEvent> {
        val startDate = LocalDateTime.now().minusDays(days.toLong())
        return events.filter { it.date.isAfter(startDate) }
    }

    // Get compliance rate (percentage of taken vs. total events)
    fun getComplianceRate(): Double {
        if (events.isEmpty()) return 0.0
        val takenCount = events.count { it.type == MedicationEvent.EventType.TAKEN }
        return (takenCount.toDouble() / events.size) * 100
    }


    fun hasEventToday(): Boolean {
        val today = LocalDate.now()
        return events.any {
            it.date.toLocalDate() == today
        }


    }

    // Get count of events by type
    fun getEventCount(type: MedicationEvent.EventType): Int {
        return events.count { it.type == type }
    }

    // Clear events older than a certain date
    fun clearEventsOlderThan(date: LocalDateTime) {
        events.removeAll { it.date.isBefore(date) }
    }

    // Check if medication was taken today
    fun wasTakenToday(): Boolean {
        val today = LocalDate.now()
        return events.any {
            it.type == MedicationEvent.EventType.TAKEN &&
                    it.date.toLocalDate() == today
        }
    }

    fun isEmpty(): Boolean {
        return events.isEmpty()
    }

    fun wasSkippedToday():Boolean {
        val today = LocalDate.now()
        return events.any {
            it.type == MedicationEvent.EventType.SKIPPED &&
                    it.date.toLocalDate() == today
        }
    }

}

data class MedicationDose(
    val medication: Medication,
    val dueTime: LocalTime,
    val doseNumber: Int,
    val totalDoses: Int
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
      //  val nextDueDates: ""

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