package com.mints.mobilehealthapplication.utils

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

fun Timestamp.toLocalDateTime(): LocalDateTime {
    return this.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
}

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


    fun calculateCyclicDueDates(
        intakeDays: Int,
        pauseDays: Int,
        times: List<LocalTime>,
        currentCycleStartDate: Timestamp?
    ): Pair<List<LocalDateTime>, Timestamp> {
        val cycleStartDate: LocalDate = currentCycleStartDate?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate() ?: LocalDate.now()

        val today = LocalDate.now()
        val cycleLength = intakeDays + pauseDays

        var newCycleStartDate = cycleStartDate
        if (today.isAfter(cycleStartDate)) {
            val daysSinceStart = ChronoUnit.DAYS.between(cycleStartDate, today)
            val completedCycles = daysSinceStart / cycleLength
            if (completedCycles > 0) {
                newCycleStartDate = cycleStartDate.plusDays(completedCycles * cycleLength)
            }

            val daysIntoCurrentCycle = daysSinceStart % cycleLength
        }

        val newDueDates = mutableListOf<LocalDateTime>()
        for (day in 0 until intakeDays) {
            val date = newCycleStartDate.plusDays(day.toLong())
            times.forEach { time ->
                newDueDates.add(LocalDateTime.of(date, time))
            }
        }

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