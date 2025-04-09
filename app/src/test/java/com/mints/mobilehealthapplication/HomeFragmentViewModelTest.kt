package com.mints.mobilehealthapplication.viewmodels

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.Timestamp
import com.mints.mobilehealthapplication.data.DailyFrequency
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationHistory
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date

@ExperimentalCoroutinesApi
class HomeFragmentViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeFragmentViewModel
    private lateinit var mockNotificationHelper: NotificationHelper
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockNotificationHelper = mockk {
            every { getContext() } returns mockContext
            every { cancelBackupNotification(any()) } returns Unit
            every { cancelRegularNotification(any(), any()) } returns Unit
        }

        mockkObject(FireStoreRepository)

        viewModel = HomeFragmentViewModel(mockNotificationHelper, initialiseWorker = false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `markMedicationAsTaken updates medication and repository`() = runTest {
        // Given
        val userId = "test-user-id"
        val medicationId = "med-123"
        val medicationName = "Test Medication"

        val nextDueDate = LocalDateTime.now()
        val times = listOf(LocalTime.of(8, 0))

        val medicationHistory = MedicationHistory()

        val medication = Medication(
            id = medicationId,
            name = medicationName,
            dosage = "10mg",
            schedule = MedicationSchedule.Daily(
                frequency = DailyFrequency.ONCE,
                times = times,
                nextDueDates = listOf(nextDueDate)
            ),
            medicationHistory = medicationHistory,
            createdAt = Timestamp(Date()),
            lastModified = Timestamp(Date())
        )

        coEvery {
            FireStoreRepository.updateMedicationHistory(
                userId = userId,
                medicationId = medicationId,
                event = any<MedicationEvent.Taken>()
            )
        } returns true

        coEvery {
            FireStoreRepository.updateAdherenceStreak(userId)
        } returns true

        viewModel.getMedicationList().value = listOf(medication)

        var completionCalled = false
        viewModel.markMedicationAsTaken(userId, medication) {
            completionCalled = true
        }

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        // Verify that an event was added to the medication history
        assertTrue("Medication history should contain at least one event",
            medicationHistory.getAllEvents().isNotEmpty())

        // Verify that the event is of type TAKEN
        assertEquals(MedicationEvent.EventType.TAKEN,
            medicationHistory.getLastEvent()?.type)

        // Verify notification cancellation
        verify { mockNotificationHelper.cancelBackupNotification(medicationName) }
        verify { mockNotificationHelper.cancelRegularNotification(medicationName, any()) }

        // Verify callback was called
        assertTrue(completionCalled)
    }

    @Test
    fun `markMedicationAsTaken handles repository failure`() = runTest {
        // Given
        val userId = "test-user-id"
        val medicationId = "med-123"
        val medicationName = "Test Medication"

        val nextDueDate = LocalDateTime.now()
        val times = listOf(LocalTime.of(8, 0))

        // Create a real MedicationHistory instance
        val medicationHistory = MedicationHistory()

        // Create a real Medication instance
        val medication = Medication(
            id = medicationId,
            name = medicationName,
            dosage = "10mg",
            schedule = MedicationSchedule.Daily(
                frequency = DailyFrequency.ONCE,
                times = times,
                nextDueDates = listOf(nextDueDate)
            ),
            medicationHistory = medicationHistory,
            createdAt = Timestamp(Date()),
            lastModified = Timestamp(Date())
        )

        // Mock repository responses - this time with failure
        coEvery {
            FireStoreRepository.updateMedicationHistory(
                userId = userId,
                medicationId = medicationId,
                event = any<MedicationEvent.Taken>()
            )
        } returns false

        // Set initial medications list
        viewModel.getMedicationList().value = listOf(medication)

        // When
        var completionCalled = false
        viewModel.markMedicationAsTaken(userId, medication) {
            completionCalled = true
        }

        // Execute pending coroutines
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        // Verify that callback was still called even on failure
        assertTrue(completionCalled)

        // Verify notification cancellation never happened
        verify(exactly = 0) { mockNotificationHelper.cancelBackupNotification(any()) }
        verify(exactly = 0) { mockNotificationHelper.cancelRegularNotification(any(), any()) }
    }

    @Test
    fun `markMedicationAsSkipped updates medication and repository`() = runTest {
        // Given
        val userId = "test-user-id"
        val medicationId = "med-123"
        val medicationName = "Test Medication"
        val nextDueDate = LocalDateTime.now()
        val times = listOf(LocalTime.of(8, 0))
        val medicationHistory = MedicationHistory()
        val medication = Medication(
            id = medicationId,
            name = medicationName,
            dosage = "10mg",
            schedule = MedicationSchedule.Daily(
                frequency = DailyFrequency.ONCE,
                times = times,
                nextDueDates = listOf(nextDueDate)
            ),
            medicationHistory = medicationHistory,
            createdAt = Timestamp(Date()),
            lastModified = Timestamp(Date())
        )

        // Mock repository response
        coEvery {
            FireStoreRepository.updateMedicationHistory(
                userId = userId,
                medicationId = medicationId,
                event = any<MedicationEvent.Skipped>()
            )
        } returns true

        // Set initial medications list
        viewModel.getMedicationList().value = listOf(medication)

        // When
        var completionCalled = false
        viewModel.markMedicationAsSkipped(userId, medication) {
            completionCalled = true
        }

        // Execute pending coroutines
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        // Verify event was added to medication history
        assertTrue(medicationHistory.getAllEvents().isNotEmpty())
        assertEquals(MedicationEvent.EventType.SKIPPED, medicationHistory.getLastEvent()?.type)
        verify { mockNotificationHelper.cancelBackupNotification(medicationName) }
        verify { mockNotificationHelper.cancelRegularNotification(medicationName, any()) }
        assertTrue(completionCalled)
    }
    @Test
    fun `undoLastTaken reverts medication to original state`() = runTest {
        // Given
        val medicationId = "med-123"
        val medicationName = "Test Medication"
        val originalDates = listOf(LocalDateTime.now())
        val times = listOf(LocalTime.of(8, 0))

        val medication = Medication(
            id = medicationId,
            name = medicationName,
            dosage = "10mg",
            schedule = MedicationSchedule.Daily(
                frequency = DailyFrequency.ONCE,
                times = times,
                nextDueDates = originalDates
            ),
            medicationHistory = MedicationHistory(),
            createdAt = Timestamp(Date()),
            lastModified = Timestamp(Date())
        )

        // Set private fields using reflection
        val lastOriginalMedicationField = HomeFragmentViewModel::class.java.getDeclaredField("_lastOriginalMedication")
        lastOriginalMedicationField.isAccessible = true
        lastOriginalMedicationField.set(viewModel, medication)

        val lastOriginalDatesField = HomeFragmentViewModel::class.java.getDeclaredField("_lastOriginalDates")
        lastOriginalDatesField.isAccessible = true
        lastOriginalDatesField.set(viewModel, originalDates)

        // Set modified medication in the list
        val modifiedMedication = medication.copy(
            schedule = (medication.schedule as MedicationSchedule.Daily).copy(
                nextDueDates = listOf(LocalDateTime.now().plusDays(1))
            )
        )
        viewModel.getMedicationList().value = listOf(modifiedMedication)

        // When
        viewModel.undoLastTaken(medication)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val updatedMedication = viewModel.getMedicationList().value?.firstOrNull()
        assertEquals(originalDates, (updatedMedication?.schedule as? MedicationSchedule.Daily)?.nextDueDates)
    }

    @Test
    fun `getMedications updates medication list on success`() = runTest {
        // Given
        val userId = "test-user-id"
        val medications = listOf(
            Medication(
                id = "med-1",
                name = "Test Med 1",
                dosage = "10mg",
                schedule = MedicationSchedule.Daily(
                    frequency = DailyFrequency.ONCE,
                    times = listOf(LocalTime.of(8, 0)),
                    nextDueDates = listOf(LocalDateTime.now())
                ),
                medicationHistory = MedicationHistory(),
                createdAt = Timestamp(Date()),
                lastModified = Timestamp(Date())
            ),
            Medication(
                id = "med-2",
                name = "Test Med 2",
                dosage = "20mg",
                schedule = MedicationSchedule.Daily(
                    frequency = DailyFrequency.TWICE,
                    times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                    nextDueDates = listOf(LocalDateTime.now(), LocalDateTime.now().plusHours(12))
                ),
                medicationHistory = MedicationHistory(),
                createdAt = Timestamp(Date()),
                lastModified = Timestamp(Date())
            )
        )

        // Mock the repository response
        every {
            FireStoreRepository.getMedicationsSnapshot(userId, captureLambda())
        } answers {
            val callback = secondArg<(List<Medication>, Exception?) -> Unit>()
            callback(medications, null)
        }

        // When
        var completionCalled = false
        viewModel.getMedications(userId) {
            completionCalled = true
        }

        // Execute pending coroutines
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(medications, viewModel.getMedicationList().value)
        assertTrue(completionCalled)
    }

    @Test
    fun `getMedications handles repository error`() = runTest {
        // Given
        val userId = "test-user-id"
        val error = Exception("Network error")

        // Mock the repository response with error
        every {
            FireStoreRepository.getMedicationsSnapshot(userId, captureLambda())
        } answers {
            val callback = secondArg<(List<Medication>, Exception?) -> Unit>()
            callback(emptyList(), error)
        }

        // Set initial state
        viewModel.getMedicationList().value = listOf(mockk())

        // When
        var completionCalled = false
        viewModel.getMedications(userId) {
            completionCalled = true
        }

        // Execute pending coroutines
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - medication list should remain unchanged on error
        assertTrue(viewModel.getMedicationList().value?.isNotEmpty() == true)
        assertTrue(completionCalled)
    }

    @Test
    fun `onMedicationClicked updates navigate event`() {
        // Given
        val medication = Medication(
            id = "med-1",
            name = "Test Med",
            dosage = "10mg",
            schedule = MedicationSchedule.Daily(
                frequency = DailyFrequency.ONCE,
                times = listOf(LocalTime.of(8, 0)),
                nextDueDates = listOf(LocalDateTime.now())
            ),
            medicationHistory = MedicationHistory(),
            createdAt = Timestamp(Date()),
            lastModified = Timestamp(Date())
        )

        // When
        viewModel.onMedicationClicked(medication)

        // Then
        assertEquals(medication, viewModel.navigateToDetails.value)
    }

    @Test
    fun `deleteMedication calls repository and completes callback`() = runTest {
        // Given
        val userId = "test-user-id"
        val medicationId = "med-123"

        coEvery {
            FireStoreRepository.deleteMedication(userId, medicationId)
        } returns Unit

        // When
        var completionCalled = false
        viewModel.deleteMedication(userId, medicationId) {
            completionCalled = true
        }

        // Execute pending coroutines
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { FireStoreRepository.deleteMedication(userId, medicationId) }
        assertTrue(completionCalled)
    }
}