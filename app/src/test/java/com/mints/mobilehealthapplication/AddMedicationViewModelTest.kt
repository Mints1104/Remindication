package com.mints.mobilehealthapplication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime


@ExperimentalCoroutinesApi
class AddMedicationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule() // For LiveData

    private lateinit var viewModel: AddMedicationViewModel
    private val testUserId = "test_user"
    private val testDispatcher = TestCoroutineDispatcher() // Test dispatcher for coroutines

    @Before
    fun setUp() {
        // Set the Main dispatcher for coroutines
        Dispatchers.setMain(testDispatcher)
        viewModel = AddMedicationViewModel()
        mockkObject(FireStoreRepository)
    }

    @After
    fun tearDown() {
        // Reset the Main dispatcher and clean up
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
        unmockkObject(FireStoreRepository)
    }

    @Test
    fun `saveMedication calls FireStoreRepository with correct medication`() = runTest {
        // Arrange: Set up expected input in the ViewModel
        viewModel.updateMedicationName("Ibuprofen")
        viewModel.updateDosage("200mg")
        viewModel.updateNotes("Take after food")
        viewModel.updateFrequency("Once Daily")
        viewModel.setSelectedTimes(listOf(LocalTime.of(9, 0)))

        // Stub the repository call
        coEvery { FireStoreRepository.saveMedication(any(), any()) } returns true

        // Act: Save medication
        viewModel.saveMedication(testUserId)
        advanceUntilIdle() // Wait for the coroutine to finish

        // Assert: Verify the repository's saveMedication was called with proper arguments
        val medicationSlot = io.mockk.slot<Medication>()
        coVerify { FireStoreRepository.saveMedication(eq(testUserId), capture(medicationSlot)) }

        val savedMedication = medicationSlot.captured
        assertEquals("Ibuprofen", savedMedication.name)
        assertEquals("200mg", savedMedication.dosage)
        assertEquals("Take after food", savedMedication.notes)
        assertTrue(savedMedication.schedule is MedicationSchedule.Daily)
    }

    @Test
    fun `validateBasicInfo returns false when medication name is empty`() {
        // Arrange
        viewModel.updateMedicationName("")
        viewModel.updateDosage("200mg")

        // Act
        val result = viewModel.validateBasicInfo()

        // Assert
        assertEquals(false, result)
        assertEquals(AddMedicationViewModel.ValidationState.Invalid("Medication name is required"), viewModel.validationState.value)
    }

    @Test
    fun `validateBasicInfo returns false when dosage is empty`() {
        // Arrange
        viewModel.updateMedicationName("Ibuprofen")
        viewModel.updateDosage("")

        // Act
        val result = viewModel.validateBasicInfo()

        // Assert
        assertEquals(false, result)
        assertEquals(AddMedicationViewModel.ValidationState.Invalid("Dosage is required"), viewModel.validationState.value)
    }

    @Test
    fun `validateBasicInfo updates form stage when valid`() {
        // Arrange
        viewModel.updateMedicationName("Ibuprofen")
        viewModel.updateDosage("200mg")

        // Act
        val result = viewModel.validateBasicInfo()

        // Assert
        assertEquals(true, result)
        assertEquals(AddMedicationViewModel.ValidationState.Valid, viewModel.validationState.value)
        assertEquals(AddMedicationViewModel.FormStage.FREQUENCY, viewModel.currentStage.value)
    }

    @Test
    fun `validateFrequency returns false when frequency is empty`() {
        // Arrange
        viewModel.updateFrequency("")

        // Act
        val result = viewModel.validateFrequency()

        // Assert
        assertEquals(false, result)
        assertEquals(AddMedicationViewModel.ValidationState.Invalid("Frequency is required"), viewModel.validationState.value)
    }

    @Test
    fun `validateFrequency updates form stage when valid`() {
        // Arrange
        viewModel.updateFrequency("Once Daily")

        // Act
        val result = viewModel.validateFrequency()

        // Assert
        assertEquals(true, result)
        assertEquals(AddMedicationViewModel.ValidationState.Valid, viewModel.validationState.value)
        assertEquals(AddMedicationViewModel.FormStage.SCHEDULE, viewModel.currentStage.value)
    }

    @Test
    fun `validateDailySchedule returns false when no times selected`() {
        // Arrange
        viewModel.updateFrequency("Once Daily")
        viewModel.setSelectedTimes(emptyList())

        // Act
        val result = viewModel.validateSchedule()

        // Assert
        assertEquals(false, result)
        assertEquals(AddMedicationViewModel.ValidationState.Invalid("Please select at least one time"), viewModel.validationState.value)
    }

    @Test
    fun `validateDailySchedule returns true when times are selected`() {
        // Arrange
        viewModel.updateFrequency("Once Daily")
        viewModel.setSelectedTimes(listOf(LocalTime.of(9, 0)))

        // Act
        val result = viewModel.validateSchedule()

        // Assert
        assertEquals(true, result)
        assertEquals(AddMedicationViewModel.ValidationState.Valid, viewModel.validationState.value)
    }

    @Test
    fun `validateWeeklySchedule returns false when no days selected`() {
        // Arrange
        viewModel.updateFrequency("Weekly")
        viewModel.setSelectedDays(emptySet())
        viewModel.setSelectedTimes(listOf(LocalTime.of(9, 0)))

        // Act
        val result = viewModel.validateSchedule()

        // Assert
        assertEquals(false, result)
        assertEquals(AddMedicationViewModel.ValidationState.Invalid("Please select at least one day"), viewModel.validationState.value)
    }

    @Test
    fun `validateWeeklySchedule returns false when no times selected`() {
                viewModel.updateFrequency("Weekly")
        viewModel.setSelectedDays(setOf(DayOfWeek.MONDAY))
        viewModel.setSelectedTimes(emptyList())

        // Act
        val result = viewModel.validateSchedule()

        // Assert
        assertEquals(false, result)
        assertEquals(AddMedicationViewModel.ValidationState.Invalid("Please select at least one time"), viewModel.validationState.value)
    }

    @Test
    fun `updateMedication calls FireStoreRepository with correct medication`() = runTest {
        // Arrange
        val medicationId = "med123"
        viewModel.updateMedicationName("Amoxicillin")
        viewModel.updateDosage("500mg")
        viewModel.updateNotes("Take with water")
        viewModel.updateFrequency("Twice Daily")
        viewModel.updateMedicationId(medicationId)
        viewModel.setSelectedTimes(listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)))
        viewModel.setIsEditing(true)

        coEvery { FireStoreRepository.updateMedication(any(), any()) } returns true

        // Act
        viewModel.updateMedication(testUserId)
        advanceUntilIdle()

        // Assert
        val medicationSlot = io.mockk.slot<Medication>()
        coVerify { FireStoreRepository.updateMedication(eq(testUserId), capture(medicationSlot)) }

        val updatedMedication = medicationSlot.captured
        assertEquals("Amoxicillin", updatedMedication.name)
        assertEquals("500mg", updatedMedication.dosage)
        assertEquals("Take with water", updatedMedication.notes)
        assertEquals(medicationId, updatedMedication.id)
        assertTrue(updatedMedication.schedule is MedicationSchedule.Daily)
    }

    @Test
    fun `validateCyclicSchedule returns false when intake days is null`() = runTest {
        // Arrange
        viewModel.updateFrequency("Cyclic")
        viewModel.updateIntakeDays(0) // Setting to 0 to simulate unset
        viewModel.updatePauseDays(2)
        viewModel.setSelectedTimes(listOf(LocalTime.of(9, 0)))

        // Act
        val result = viewModel.validateSchedule()

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `validateOnDemand returns true with valid parameters`() = runTest {
        // Arrange
        viewModel.updateFrequency("On Demand")
        viewModel.updateMaxDoses(3)
        viewModel.updateMinHoursBetween(4)

        // Act
        val result = viewModel.validateSchedule()

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `resetAllData clears all fields`() {
        // Arrange - Set values
        viewModel.updateMedicationName("Test Med")
        viewModel.updateDosage("100mg")
        viewModel.updateNotes("Test notes")
        viewModel.updateFrequency("Once Daily")

        // Act
        viewModel.resetAllData()

        // Assert
        assertEquals("", viewModel.medicationName.value)
        assertEquals("", viewModel.dosage.value)
        assertEquals("", viewModel.notes.value)
        assertEquals("", viewModel.frequency.value)
        assertEquals(AddMedicationViewModel.FormStage.BASIC_INFO, viewModel.currentStage.value)
        assertEquals(false, viewModel.isEditing.value)
    }

    @Test
    fun `getMedicationDetails loads medication info correctly`() = runTest {
        // Arrange
        val testMedId = "med123"
        val testMed = Medication(
            id = testMedId,
            name = "Aspirin",
            dosage = "100mg",
            notes = "Test notes",
            schedule = MedicationSchedule.OnDemand(
                maxDailyDoses = 4,
                minTimeBetweenDoses = 6,
                instructions = "As needed for pain"
            )
        )

        coEvery { FireStoreRepository.getMedicationDetails(any(), any()) } returns testMed

        // Act
        viewModel.getMedicationDetails(testUserId, testMedId)
        advanceUntilIdle()

        // Assert
        assertEquals("Aspirin", viewModel.medicationName.value)
        assertEquals("100mg", viewModel.dosage.value)
        assertEquals("Test notes", viewModel.notes.value)
        assertEquals("On Demand", viewModel.frequency.value)
        assertEquals("On Demand", viewModel.frequencyType.value)
        assertEquals(testMedId, viewModel.medicationId.value)
    }
}