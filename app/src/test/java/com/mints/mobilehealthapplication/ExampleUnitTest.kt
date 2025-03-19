package com.mints.mobilehealthapplication

import com.google.firebase.Timestamp
import com.mints.mobilehealthapplication.utils.ScheduleHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ScheduleHelperTest {

    @Test
    fun calculateCyclicDueDates_shouldReturnCorrectNextDueDates() {
        // Arrange - Set up test data
        val intakeDays = 3
        val pauseDays = 2
        val times = listOf(LocalTime.of(9, 0), LocalTime.of(18, 0))
        val startDate = Timestamp.now() // Assume today

        // Act - Call function
        val result = ScheduleHelper.calculateCyclicDueDates(intakeDays, pauseDays, times, startDate)

        // Assert - Verify results
        assertEquals(6, result.size) // 3 days * 2 times per day
        assertEquals(LocalDate.now(), result[0].toLocalDate()) // First day
        assertEquals(LocalDate.now().plusDays(1), result[2].toLocalDate()) // Second day
    }

    @Test
    fun calculateCyclicDueDates_shouldHandleCycleTransitionsCorrectly() {
        val intakeDays = 3
        val pauseDays = 2
        val times = listOf(LocalTime.of(9, 0))
        val pastStartDate = Timestamp.now().toDate().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate().minusDays(10) // Simulate a past cycle start

        val result = ScheduleHelper.calculateCyclicDueDates(
            intakeDays, pauseDays, times, Timestamp(pastStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond, 0)
        )

        assertEquals(3, result.size) // Should still only return intake days
        assertEquals(LocalDate.now(), result[0].toLocalDate()) // Should adjust to current cycle
    }

    @Test
    fun calculateCyclicDueDates_shouldSkipPauseDays() {
        val intakeDays = 4
        val pauseDays = 3
        val times = listOf(LocalTime.of(12, 0))
        val startDate = Timestamp.now()

        val result = ScheduleHelper.calculateCyclicDueDates(intakeDays, pauseDays, times, startDate)

        val expectedDays = listOf(
            LocalDate.now(),
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            LocalDate.now().plusDays(3)
        )

        assertEquals(4, result.size)
        expectedDays.forEachIndexed { index, expectedDate ->
            assertEquals(expectedDate, result[index].toLocalDate())
        }
    }

    @Test
    fun adjustDailyDueDate_shouldReturnDateAfterNow() {
        // Arrange: Set a due date that is in the past relative to now
        val now = LocalDateTime.now()
        // Let's assume the due date is 2 days ago at 9:00 AM
        val pastDateTime = now.minusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0)

        // Act: Adjust the due date
        val adjustedDateTime = ScheduleHelper.adjustDailyDueDate(pastDateTime, now)

        // Assert: The adjusted due date should be in the future (or at least equal to now)
        // It should be exactly the first future date at 9:00 AM
        var expectedDateTime = pastDateTime
        while (expectedDateTime.isBefore(now)) {
            expectedDateTime = expectedDateTime.plusDays(1)
        }
        assertEquals(expectedDateTime, adjustedDateTime)
    }

    @Test
    fun adjustWeeklyDueDate_shouldReturnDateAfterNow() {
        // Arrange: Set a weekly due date in the past relative to now
        val now = LocalDateTime.now()
        // Let’s say the due date is last week at 10:00 AM
        val pastDateTime = now.minusWeeks(1).withHour(10).withMinute(0).withSecond(0).withNano(0)

        // Act: Adjust the weekly due date
        val adjustedDateTime = ScheduleHelper.adjustWeeklyDueDate(pastDateTime, now)

        // Assert: The adjusted due date should be in the future (or equal to now)
        var expectedDateTime = pastDateTime
        while (expectedDateTime.isBefore(now)) {
            expectedDateTime = expectedDateTime.plusDays(7)
        }
        assertEquals(expectedDateTime, adjustedDateTime)
    }

    @Test
    fun getDatesBetween_shouldReturnAllDatesInclusive() {
        // Arrange: Define a start and end date
        val start = LocalDate.of(2025, 3, 1)
        val end = LocalDate.of(2025, 3, 5)  // Expected dates: 1, 2, 3, 4, 5

        // Act: Get dates between
        val result = ScheduleHelper.getDatesBetween(start, end)

        // Assert: List should contain exactly 5 dates
        val expectedDates = listOf(
            LocalDate.of(2025, 3, 1),
            LocalDate.of(2025, 3, 2),
            LocalDate.of(2025, 3, 3),
            LocalDate.of(2025, 3, 4),
            LocalDate.of(2025, 3, 5)
        )
        assertEquals(expectedDates, result)
    }

}
