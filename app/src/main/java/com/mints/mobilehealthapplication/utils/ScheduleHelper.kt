package com.mints.mobilehealthapplication.utils

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

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
    ): List<LocalDateTime> {
        // Convert Timestamp to LocalDate (default to today if null)
        val cycleStartDate: LocalDate = currentCycleStartDate?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate() ?: LocalDate.now()

        val today = LocalDate.now()
        val cycleLength = intakeDays + pauseDays
        val cycleEndDate = cycleStartDate.plusDays(intakeDays.toLong())

        // Advance cycle if today is on or after the cycle end date
        val newCycleStartDate = if (today.isAfter(cycleEndDate) || today.isEqual(cycleEndDate)) {
            cycleStartDate.plusDays(cycleLength.toLong())
        } else {
            cycleStartDate
        }

        val newDueDates = mutableListOf<LocalDateTime>()
        for (day in 0 until intakeDays) {
            val date = newCycleStartDate.plusDays(day.toLong())
            times.forEach { time ->
                newDueDates.add(LocalDateTime.of(date, time))
            }
        }
        return newDueDates.sorted()
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