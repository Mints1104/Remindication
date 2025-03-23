package com.mints.mobilehealthapplication.utils

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

object ScheduleHelper {

    fun adjustDailyDueDate(dueDate: LocalDateTime, now: LocalDateTime): LocalDateTime {
        var newDueDate = dueDate
        while (newDueDate.isBefore(now)) {
            newDueDate = newDueDate.plusDays(1)
        }
        return newDueDate
    }

    fun adjustWeeklyDueDate(dueDate: LocalDateTime, now: LocalDateTime): LocalDateTime {
        var newDueDate = dueDate
        while (newDueDate.isBefore(now)) {
            newDueDate = newDueDate.plusDays(7)
        }
        return newDueDate
    }

    /**
     * Calculates the next due dates for a cyclic medication schedule.
     * If the current cycle has ended, a new cycle starts based on the intake & pause period.
     *
     * @param intakeDays Number of days the medication should be taken
     * @param pauseDays Number of days without medication after an intake cycle
     * @param times List of times during the day when medication should be taken
     * @param currentCycleStartDate Timestamp marking the beginning of the cycle
     *
     * @return List of LocalDateTime objects representing the new due dates.
     */
    fun calculateCyclicDueDates(
        intakeDays: Int,
        pauseDays: Int,
        times: List<LocalTime>,
        currentCycleStartDate: Timestamp?
    ): Pair<List<LocalDateTime>, Timestamp> {  // Return both due dates and the new cycle start timestamp
        // Convert Timestamp to LocalDate (default to today if null)
        val cycleStartDate: LocalDate = currentCycleStartDate?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate() ?: LocalDate.now()

        val today = LocalDate.now()
        val cycleLength = intakeDays + pauseDays

        // Calculate how many full cycles have passed since the start date
        var newCycleStartDate = cycleStartDate
        if (today.isAfter(cycleStartDate)) {
            val daysSinceStart = ChronoUnit.DAYS.between(cycleStartDate, today)
            val completedCycles = daysSinceStart / cycleLength
            // If cycles completed, advance the start date by that many cycles
            if (completedCycles > 0) {
                newCycleStartDate = cycleStartDate.plusDays(completedCycles * cycleLength)
            }

            // Check if we're in the pause period of the current cycle
            val daysIntoCurrentCycle = daysSinceStart % cycleLength
            if (daysIntoCurrentCycle >= intakeDays) {
                // We're in the pause period, the next active cycle hasn't started yet
                // No need to advance further
            }
        }

        val newDueDates = mutableListOf<LocalDateTime>()
        for (day in 0 until intakeDays) {
            val date = newCycleStartDate.plusDays(day.toLong())
            times.forEach { time ->
                newDueDates.add(LocalDateTime.of(date, time))
            }
        }

        // Convert new cycle start date back to Timestamp
        val newCycleStartTimestamp = Timestamp(
            Date.from(
                newCycleStartDate.atStartOfDay()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            )
        )

        return Pair(newDueDates.sorted(), newCycleStartTimestamp)
    }



    fun getDatesBetween(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start
        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusDays(1)
        }
        return dates
    }
}