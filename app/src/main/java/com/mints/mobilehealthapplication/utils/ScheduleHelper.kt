package com.mints.mobilehealthapplication.utils

import java.time.LocalDate
import java.time.LocalDateTime

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