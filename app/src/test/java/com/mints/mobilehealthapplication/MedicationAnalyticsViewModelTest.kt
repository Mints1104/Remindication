package com.mints.mobilehealthapplication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationHistory
import com.mints.mobilehealthapplication.viewmodels.MedicationAnalyticsViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class MedicationAnalyticsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: MedicationAnalyticsViewModel

    // Mock data
    private val testUserId = "test-user-123"
    private val testMedication1 = mockk<Medication>()
    private val testMedication2 = mockk<Medication>()
    private val testMedications = listOf(testMedication1, testMedication2)
    private val testHistory1 = mockk<MedicationHistory>()
    private val testHistory2 = mockk<MedicationHistory>()
    private val testEvents1 = listOf(
        mockk<MedicationEvent.Taken> { every { date } returns LocalDateTime.now().minusSeconds(86400) },
        mockk<MedicationEvent.Missed> { every { date } returns LocalDateTime.now().minusSeconds(172800) }
    )
    private val testEvents2 = listOf(
        mockk<MedicationEvent.Taken> { every { date } returns LocalDateTime.now().minusSeconds(86400) },
        mockk<MedicationEvent.Taken> { every { date } returns LocalDateTime.now().minusSeconds(172800) }
    )
    private val allTestEvents = testEvents1 + testEvents2

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(FireStoreRepository)

        // Setup mock medications
        every { testMedication1.id } returns "med-1"
        every { testMedication1.medicationHistory } returns testHistory1
        every { testHistory1.getAllEvents() } returns testEvents1.toMutableList()

        every { testMedication2.id } returns "med-2"
        every { testMedication2.medicationHistory } returns testHistory2
        every { testHistory2.getAllEvents() } returns testEvents2.toMutableList()

        viewModel = MedicationAnalyticsViewModel()
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
        assertEquals(allTestEvents, viewModel.medicationEvents.value)
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
        // The ViewModel doesn't update medications when there's an error
        assertNull(viewModel.medications.value)
        assertNull(viewModel.medicationEvents.value)
    }

    @Test
    fun `calculateOverallMedicationAdherence should update medicationEvents LiveData`() {
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
        viewModel.getMedications(testUserId)

        // Verify medicationEvents LiveData was updated with all events
        assertEquals(allTestEvents, viewModel.medicationEvents.value)
    }

    @Test
    fun `calculateOverallMedicationAdherence should handle empty medications list`() {
        // Mock the repository response with empty list
        every {
            FireStoreRepository.getMedicationsSnapshot(
                eq(testUserId),
                captureLambda()
            )
        } answers {
            lambda<(List<Medication>, Exception?) -> Unit>().invoke(emptyList(), null)
        }

        // Call the method being tested
        viewModel.getMedications(testUserId)

        // Verify medicationEvents LiveData was updated with empty list
        assertEquals(emptyList<MedicationEvent>(), viewModel.medicationEvents.value)
        assertEquals(0f, viewModel.adherencePercentage.value)
    }

    @Test
    fun `getMedications should collect all events from all medications`() {
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
        viewModel.getMedications(testUserId)

        // Verify results
        val expectedEvents = testEvents1 + testEvents2
        assertEquals(expectedEvents, viewModel.medicationEvents.value)
    }

    @Test
    fun `adherencePercentage should be zero for empty medications list`() {
        // Mock the repository response with empty list
        every {
            FireStoreRepository.getMedicationsSnapshot(
                eq(testUserId),
                captureLambda()
            )
        } answers {
            lambda<(List<Medication>, Exception?) -> Unit>().invoke(emptyList(), null)
        }

        // Call the method being tested
        viewModel.getMedications(testUserId)

        // Verify adherencePercentage LiveData
        assertEquals(0f, viewModel.adherencePercentage.value)
    }
}