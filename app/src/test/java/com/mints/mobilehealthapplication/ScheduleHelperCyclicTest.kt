package com.mints.mobilehealthapplication

import com.google.firebase.Timestamp
import com.mints.mobilehealthapplication.utils.ScheduleHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

class ScheduleHelperCyclicTest {

    // Test when currentCycleStartDate is null: the function should default to today.
    @Test
    fun calculateCyclicDueDates_withNullCycleStartDate_shouldDefaultToToday() {
        // Arrange:
        val intakeDays = 3
        val pauseDays = 2
        val times = listOf(
            LocalTime.of(9, 0),
            LocalTime.of(15, 30)
        )
        // When no cycle start date is provided, today is used.
        val cycleStartDate = LocalDate.now()

        // Expected: For each day within the intake period, build the list of due dates.
        val expectedDueDates = mutableListOf<LocalDateTime>()
        for (day in 0 until intakeDays) {
            val date = cycleStartDate.plusDays(day.toLong())
            times.forEach { time ->
                expectedDueDates.add(LocalDateTime.of(date, time))
            }
        }
        val expectedSortedDates = expectedDueDates.sorted()

        // The new cycle start timestamp should be the cycleStartDate at the start of day.
        val expectedCycleStartTimestamp = Timestamp(
            Date.from(
                cycleStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        // Act: Invoke calculateCyclicDueDates with a null Timestamp.
        val (resultDueDates, resultCycleStartTimestamp) =
            ScheduleHelper.calculateCyclicDueDates(intakeDays, pauseDays, times, null)

        // Assert: Verify that the due dates and the new cycle start timestamp are as expected.
        assertEquals(expectedSortedDates, resultDueDates)
        assertEquals(expectedCycleStartTimestamp, resultCycleStartTimestamp)
    }

    // Test when a valid currentCycleStartDate is provided.
    @Test
    fun calculateCyclicDueDates_withProvidedCycleStartDate_shouldCalculateNewCycle() {
        // Arrange:
        val intakeDays = 3
        val pauseDays = 2
        val times = listOf(
            LocalTime.of(8, 0),
            LocalTime.of(20, 0)
        )
        val cycleLength = intakeDays + pauseDays

        // For reproducibility, choose a cycle start date relative to today.
        // For instance, let’s say the cycle start was 7 days ago.
        val fixedStartDate = LocalDate.now().minusDays(7)
        val fixedCycleStartTimestamp = Timestamp(
            Date.from(
                fixedStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        // Calculate expected new cycle start date:
        // daysSinceStart = number of days between fixedStartDate and today.
        val daysSinceStart = ChronoUnit.DAYS.between(fixedStartDate, LocalDate.now())
        // Determine how many full cycles have been completed.
        val completedCycles = daysSinceStart / cycleLength
        var newCycleStartDate = fixedStartDate
        if (completedCycles > 0) {
            newCycleStartDate = fixedStartDate.plusDays(completedCycles * cycleLength)
        }
        // Determine if we're in the pause period.
        val daysIntoCurrentCycle = daysSinceStart % cycleLength
        // If daysIntoCurrentCycle >= intakeDays, we’re still in the pause period;
        // In this implementation, no further advance is done.
        // The due dates are generated based on newCycleStartDate.
        //
        // Expected due dates: For each day in the active intake period starting from newCycleStartDate.
        val expectedDueDates = mutableListOf<LocalDateTime>()
        for (day in 0 until intakeDays) {
            val date = newCycleStartDate.plusDays(day.toLong())
            times.forEach { time ->
                expectedDueDates.add(LocalDateTime.of(date, time))
            }
        }
        val expectedSortedDates = expectedDueDates.sorted()

        // The new cycle start timestamp is the newCycleStartDate at start of day.
        val expectedCycleStartTimestamp = Timestamp(
            Date.from(
                newCycleStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        // Act: Call the function with the provided cycle start timestamp.
        val (resultDueDates, resultCycleStartTimestamp) =
            ScheduleHelper.calculateCyclicDueDates(intakeDays, pauseDays, times, fixedCycleStartTimestamp)

        // Assert: Verify that the calculated due dates and new cycle start timestamp match the expected values.
        assertEquals(expectedSortedDates, resultDueDates)
        assertEquals(expectedCycleStartTimestamp, resultCycleStartTimestamp)
    }
}
