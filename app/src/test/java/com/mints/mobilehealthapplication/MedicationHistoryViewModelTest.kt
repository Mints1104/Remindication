package com.mints.mobilehealthapplication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationHistory
import com.mints.mobilehealthapplication.viewmodels.MedicationHistoryViewModel
import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class MedicationHistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: MedicationHistoryViewModel

    // Mock data
    private val testUserId = "test-user-123"
    private val testMedication1 = mockk<Medication>()
    private val testMedication2 = mockk<Medication>()
    private lateinit var testMedications: List<Medication>
    private val testHistory1 = mockk<MedicationHistory>()
    private val testHistory2 = mockk<MedicationHistory>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(FireStoreRepository)

        // Create mock medication events
        val takenEvents1 = mutableListOf<MedicationEvent>(
            mockk { every { type } returns MedicationEvent.EventType.TAKEN },
            mockk { every { type } returns MedicationEvent.EventType.MISSED }
        )

        val takenEvents2 = mutableListOf<MedicationEvent>(
            mockk { every { type } returns MedicationEvent.EventType.TAKEN },
            mockk { every { type } returns MedicationEvent.EventType.TAKEN }
        )

        // Setup mock medications with the events
        every { testMedication1.id } returns "med-1"
        every { testMedication1.medicationHistory.getAllEvents() } returns takenEvents1

        every { testMedication2.id } returns "med-2"
        every { testMedication2.medicationHistory.getAllEvents() } returns takenEvents2

        testMedications = listOf(testMedication1, testMedication2)

        viewModel = MedicationHistoryViewModel()
    }
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `getMedications should update medications LiveData`() {
        // Mock the repository response
        every {
            FireStoreRepository.getMedicationsSnapshot(
                eq(testUserId),
                captureLambda()
            )
        } answers {
            lambda<(List<Medication>, Exception?) -> Unit>().invoke(testMedications, null)
        }

        // Call the method being tested
        var completionCalled = false
        viewModel.getMedications(testUserId) {
            completionCalled = true
        }

        // Verify results
        assertTrue(completionCalled)
        assertEquals(testMedications, viewModel.medications.value)
    }

    @Test
    fun `getMedications should handle errors`() {
        // Mock the repository error response
        val testException = Exception("Test error")
        every {
            FireStoreRepository.getMedicationsSnapshot(
                eq(testUserId),
                captureLambda()
            )
        } answers {
            lambda<(List<Medication>, Exception?) -> Unit>().invoke(emptyList(), testException)
        }

        // Call the method being tested
        var completionCalled = false
        viewModel.getMedications(testUserId) {
            completionCalled = true
        }

        // Verify results
        assertTrue(completionCalled)
        assertEquals(emptyList<Medication>(), viewModel.medications.value)
    }

    @Test
    fun `getComplianceRate should calculate correct compliance`() {
        // Use reflection to set the medications
        val field = MedicationHistoryViewModel::class.java.getDeclaredField("_medications")
        field.isAccessible = true
        field.set(viewModel, MutableLiveData(testMedications))

        // Call the method under test
        val result = viewModel.getComplianceRate()

        // Calculate expected: 3 TAKEN out of 4 total = 75%
        assertEquals(75.0, result, 0.01)
    }

    @Test
    fun `getComplianceRate should return zero for empty medication list`() {
        // Setup - Set empty medications list
        every {
            FireStoreRepository.getMedicationsSnapshot(
                any(),
                captureLambda()
            )
        } answers {
            lambda<(List<Medication>, Exception?) -> Unit>().invoke(emptyList(), null)
        }

        viewModel.getMedications(testUserId)

        // Call the method
        val result = viewModel.getComplianceRate()

        // Verify
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `onMedicationClicked should update navigateToDetails LiveData`() {
        // Call the method
        viewModel.onMedicationClicked(testMedication1)

        // Verify
        assertEquals(testMedication1, viewModel.navigateToDetails.value)
    }

    @Test
    fun `onMedicationNavigated should clear navigateToDetails LiveData`() {
        // Setup
        viewModel.onMedicationClicked(testMedication1)
        assertEquals(testMedication1, viewModel.navigateToDetails.value)

        // Call the method
        viewModel.onMedicationNavigated()

        // Verify
        assertNull(viewModel.navigateToDetails.value)
    }

    @Test
    fun `getMedicationList should return the medications LiveData`() {
        // Setup
        every {
            FireStoreRepository.getMedicationsSnapshot(
                any(),
                captureLambda()
            )
        } answers {
            lambda<(List<Medication>, Exception?) -> Unit>().invoke(testMedications, null)
        }

        viewModel.getMedications(testUserId)

        // Call the method
        val result = viewModel.getMedicationList()

        // Verify
        assertEquals(testMedications, result.value)
    }

    @Test
    fun `testReceivingMedicationHistory logs appropriate data`() {
        // This test is limited since the method primarily logs information
        // We can verify it doesn't crash with proper mocks

        val testEvent = mockk<MedicationEvent.Taken>()
        every { testEvent.date } returns LocalDateTime.now()

        every { testHistory1.getLastEventOfType(MedicationEvent.EventType.TAKEN) } returns testEvent
        every { testHistory1.wasTakenToday() } returns true
        every { testHistory1.getEventsFromLastDays(7) } returns listOf(testEvent)

        // Method should complete without exceptions
        viewModel.testReceivingMedicationHistory(testMedication1)
    }
}