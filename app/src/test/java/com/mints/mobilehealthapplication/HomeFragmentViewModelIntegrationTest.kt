package com.mints.mobilehealthapplication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.Timestamp
import com.mints.mobilehealthapplication.data.DailyFrequency
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationHistory
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.viewmodels.HomeFragmentViewModel
import io.mockk.coEvery
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date

@ExperimentalCoroutinesApi
class HomeFragmentViewModelIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeFragmentViewModel
    private lateinit var mockNotificationHelper: NotificationHelper

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockNotificationHelper = mockk(relaxed = true) {
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
    fun `fetch medications and mark one as taken`() = runTest {
        // Given
        val userId = "test-user-id"
        val medication1 = createTestMedication("med-1", "Aspirin")
        val medication2 = createTestMedication("med-2", "Ibuprofen")
        val medications = listOf(medication1, medication2)

        // Mock repository responses
        every {
            FireStoreRepository.getMedicationsSnapshot(eq(userId), captureLambda())
        } answers {
            val callback = secondArg<(List<Medication>, Exception?) -> Unit>()
            callback(medications, null)
        }

        coEvery {
            FireStoreRepository.updateMedicationHistory(
                userId = userId,
                medicationId = medication1.id!!,
                event = any()
            )
        } returns true

        coEvery {
            FireStoreRepository.updateAdherenceStreak(userId)
        } returns true

        // When - Fetch medications
        viewModel.getMedications(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Verify medications were loaded
        assertEquals(medications, viewModel.getMedicationList().value)

        // When - Mark a medication as taken
        var completionCalled = false
        viewModel.markMedicationAsTaken(userId, medication1) {
            completionCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Verify medication was updated and notifications were canceled
        assertEquals(true, completionCalled)
        verify { mockNotificationHelper.cancelBackupNotification(medication1.name) }
        verify { mockNotificationHelper.cancelRegularNotification(medication1.name, any()) }

        // And medication history should contain the taken event
        val updatedMedication = viewModel.getMedicationList().value?.find { it.id == medication1.id }
        assertEquals(1, updatedMedication?.medicationHistory?.getAllEvents()?.size)
    }

    private fun createTestMedication(id: String, name: String): Medication {
        return Medication(
            id = id,
            name = name,
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
    }

    @Test
    fun `fetch medications and mark one as skipped`() = runTest {
        // Given
        val userId = "test-user-id"
        val medication1 = createTestMedication("med-1", "Aspirin")
        val medication2 = createTestMedication("med-2", "Ibuprofen")
        val medications = listOf(medication1, medication2)

        // Mock repository responses
        every {
            FireStoreRepository.getMedicationsSnapshot(eq(userId), captureLambda())
        } answers {
            val callback = secondArg<(List<Medication>, Exception?) -> Unit>()
            callback(medications, null)
        }

        coEvery {
            FireStoreRepository.updateMedicationHistory(
                userId = userId,
                medicationId = medication2.id!!,
                event = any()
            )
        } returns true

        // When - Fetch medications
        viewModel.getMedications(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Verify medications were loaded
        assertEquals(medications, viewModel.getMedicationList().value)

        // When - Mark a medication as skipped
        var completionCalled = false
        viewModel.markMedicationAsSkipped(userId, medication2) {
            completionCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Verify medication was updated and notifications were canceled
        assertEquals(true, completionCalled)
        verify { mockNotificationHelper.cancelBackupNotification(medication2.name) }
        verify { mockNotificationHelper.cancelRegularNotification(medication2.name, any()) }

        // And medication history should contain the skipped event
        val updatedMedication = viewModel.getMedicationList().value?.find { it.id == medication2.id }
        assertEquals(1, updatedMedication?.medicationHistory?.getAllEvents()?.size)
    }

    @Test
    fun `test deletion of medication`() = runTest {
        val userId = "test-user-id"
        val medication = createTestMedication("med-1", "Aspirin")
        val medications = listOf(medication)
        every {
            FireStoreRepository.getMedicationsSnapshot(eq(userId), captureLambda())
        } answers {
            val callback = secondArg<(List<Medication>, Exception?) -> Unit>()
            callback(medications, null)
        }

        coEvery {
            FireStoreRepository.deleteMedication(userId, medication.id!!)
        } returns Unit

        viewModel.getMedications(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(medications, viewModel.getMedicationList().value)

        var deletionCompletionCalled = false
        viewModel.deleteMedication(userId, medication.id!!) {
            deletionCompletionCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, deletionCompletionCalled)
    }

    @Test
    fun `test medication click navigation`() = runTest {
        // Given
        val medication = createTestMedication("med-1", "Aspirin")

        // When - Click on medication
        viewModel.onMedicationClicked(medication)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Verify navigation event was triggered with correct medication
        assertEquals(medication, viewModel.navigateToDetails.value)
    }

    @Test
    fun `test undo last taken medication`() = runTest {
        // Given
        val userId = "test-user-id"
        val originalDates = listOf(LocalDateTime.now().plusHours(2))
        val medication = createTestMedication("med-1", "Aspirin").apply {
            schedule = (schedule as MedicationSchedule.Daily).copy(nextDueDates = originalDates)
        }
        val medications = listOf(medication)

        // Mock repository responses
        every {
            FireStoreRepository.getMedicationsSnapshot(eq(userId), captureLambda())
        } answers {
            val callback = secondArg<(List<Medication>, Exception?) -> Unit>()
            callback(medications, null)
        }

        coEvery {
            FireStoreRepository.updateMedicationHistory(
                userId = userId,
                medicationId = medication.id!!,
                event = any()
            )
        } returns true

        coEvery {
            FireStoreRepository.updateAdherenceStreak(userId)
        } returns true

        // When - Fetch medications
        viewModel.getMedications(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Mark medication as taken (this should save the original state)
        viewModel.markMedicationAsTaken(userId, medication) {}
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Undo the action
        viewModel.undoLastTaken(medication)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Verify the dates were restored
        val updatedMedication = viewModel.getMedicationList().value?.find { it.id == medication.id }
        val updatedSchedule = updatedMedication?.schedule as MedicationSchedule.Daily
        assertEquals(originalDates, updatedSchedule.nextDueDates)
    }


}