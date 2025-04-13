package com.mints.mobilehealthapplication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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
import java.time.DayOfWeek
import java.time.LocalTime

@ExperimentalCoroutinesApi
class AddMedicationViewModelIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AddMedicationViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(FireStoreRepository)
        viewModel = AddMedicationViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test basic information validation`() {
        // Given
        viewModel.updateMedicationName("")
        viewModel.updateDosage("")

        // When - validate with empty fields
        val emptyResult = viewModel.validateBasicInfo()

        // Then
        assertEquals(false, emptyResult)
        assertEquals("Medication name is required",
            (viewModel.validationState.value as AddMedicationViewModel.ValidationState.Invalid).message)

        // When - update name but keep dosage empty
        viewModel.updateMedicationName("Aspirin")
        val nameOnlyResult = viewModel.validateBasicInfo()

        // Then
        assertEquals(false, nameOnlyResult)
        assertEquals("Dosage is required",
            (viewModel.validationState.value as AddMedicationViewModel.ValidationState.Invalid).message)

        // When - complete all required fields
        viewModel.updateDosage("10mg")
        val validResult = viewModel.validateBasicInfo()

        // Then
        assertEquals(true, validResult)
        assertTrue(viewModel.validationState.value is AddMedicationViewModel.ValidationState.Valid)
        assertEquals(AddMedicationViewModel.FormStage.FREQUENCY, viewModel.currentStage.value)
    }

    @Test
    fun `test frequency validation`() {
        // Given
        viewModel.updateFrequency("")

        // When - validate with empty frequency
        val emptyResult = viewModel.validateFrequency()

        // Then
        assertEquals(false, emptyResult)
        assertEquals("Frequency is required",
            (viewModel.validationState.value as AddMedicationViewModel.ValidationState.Invalid).message)

        // When - set valid frequency
        viewModel.updateFrequency("Once Daily")
        val validResult = viewModel.validateFrequency()

        // Then
        assertEquals(true, validResult)
        assertTrue(viewModel.validationState.value is AddMedicationViewModel.ValidationState.Valid)
        assertEquals(AddMedicationViewModel.FormStage.SCHEDULE, viewModel.currentStage.value)
    }

    @Test
    fun `test daily schedule validation`() {
        // Given
        viewModel.updateFrequency("Once Daily")
        viewModel.setSelectedTimes(emptyList())

        // When - validate with no times selected
        val emptyResult = viewModel.validateSchedule()

        // Then
        assertEquals(false, emptyResult)
        assertEquals("Please select at least one time",
            (viewModel.validationState.value as AddMedicationViewModel.ValidationState.Invalid).message)

        // When - select times
        viewModel.setSelectedTimes(listOf(LocalTime.of(8, 0)))
        val validResult = viewModel.validateSchedule()

        // Then
        assertEquals(true, validResult)
        assertTrue(viewModel.validationState.value is AddMedicationViewModel.ValidationState.Valid)
    }

    @Test
    fun `test weekly schedule validation`() {
        // Given
        viewModel.updateFrequency("Weekly")

        // When - validate with no days or times selected
        val emptyResult = viewModel.validateSchedule()

        // Then
        assertEquals(false, emptyResult)
        assertEquals("Please select at least one day",
            (viewModel.validationState.value as AddMedicationViewModel.ValidationState.Invalid).message)

        // When - select days but no times
        viewModel.setSelectedDays(setOf(DayOfWeek.MONDAY))
        val daysOnlyResult = viewModel.validateSchedule()

        // Then
        assertEquals(false, daysOnlyResult)
        assertEquals("Please select at least one time",
            (viewModel.validationState.value as AddMedicationViewModel.ValidationState.Invalid).message)

        // When - select both days and times
        viewModel.setSelectedTimes(listOf(LocalTime.of(8, 0)))
        val validResult = viewModel.validateSchedule()

        // Then
        assertEquals(true, validResult)
        assertTrue(viewModel.validationState.value is AddMedicationViewModel.ValidationState.Valid)
    }

    @Test
    fun `test cyclic schedule validation`() {
        // Given
        viewModel.updateFrequency("Cyclic")

        // When - validate with no parameters
        val emptyResult = viewModel.validateSchedule()

        // Then
        assertEquals(false, emptyResult)
        assertEquals("Please enter intake days",
            (viewModel.validationState.value as AddMedicationViewModel.ValidationState.Invalid).message)

        // When - set intake days but no pause days
        viewModel.updateIntakeDays(5)
        val intakeOnlyResult = viewModel.validateSchedule()

        // Then
        assertEquals(false, intakeOnlyResult)
        assertEquals("Please enter pause days",
            (viewModel.validationState.value as AddMedicationViewModel.ValidationState.Invalid).message)

        // When - set pause days but no times
        viewModel.updatePauseDays(2)
        val noTimesResult = viewModel.validateSchedule()

        // Then
        assertEquals(false, noTimesResult)
        assertEquals("Please select at least one time",
            (viewModel.validationState.value as AddMedicationViewModel.ValidationState.Invalid).message)

        // When - set all required parameters
        viewModel.setSelectedTimes(listOf(LocalTime.of(8, 0)))
        val validResult = viewModel.validateSchedule()

        // Then
        assertEquals(true, validResult)
        assertTrue(viewModel.validationState.value is AddMedicationViewModel.ValidationState.Valid)
    }

    @Test
    fun `test save medication success`() = runTest {
        // Given
        val userId = "test-user-id"
        viewModel.updateMedicationName("Test Medication")
        viewModel.updateDosage("10mg")
        viewModel.updateFrequency("Once Daily")
        viewModel.setSelectedTimes(listOf(LocalTime.of(8, 0)))

        // Mock repository
        coEvery {
            FireStoreRepository.saveMedication(eq(userId), any())
        } returns true

        // When
        viewModel.saveMedication(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(true, viewModel.saveResult.value)
        assertEquals("", viewModel.medicationName.value) // Should be reset after save
    }

    @Test
    fun `test update medication success`() = runTest {
        // Given
        val userId = "test-user-id"
        val medicationId = "med-123"
        viewModel.updateMedicationId(medicationId)
        viewModel.updateMedicationName("Updated Medication")
        viewModel.updateDosage("20mg")
        viewModel.updateFrequency("Once Daily")
        viewModel.setSelectedTimes(listOf(LocalTime.of(9, 0)))
        viewModel.setIsEditing(true)

        // Mock repository
        coEvery {
            FireStoreRepository.updateMedication(eq(userId), any())
        } returns true

        // When
        viewModel.updateMedication(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(true, viewModel.saveResult.value)
        assertEquals("", viewModel.medicationName.value) // Should be reset after update
    }

    @Test
    fun `test get medication details`() = runTest {
        // Given
        val userId = "test-user-id"
        val medicationId = "med-123"
        val testMedication = mockk<Medication> {
            every { id } returns medicationId
            every { name } returns "Aspirin"
            every { dosage } returns "10mg"
            every { notes } returns "Take with food"
            every { schedule } returns mockk {
                every { formattedFrequency } returns "Once Daily"
                every { frequencyType } returns "Daily"
            }
        }

        coEvery {
            FireStoreRepository.getMedicationDetails(userId, medicationId)
        } returns testMedication

        // When
        viewModel.getMedicationDetails(userId, medicationId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("Aspirin", viewModel.medicationName.value)
        assertEquals("10mg", viewModel.dosage.value)
        assertEquals("Take with food", viewModel.notes.value)
        assertEquals("Once Daily", viewModel.frequency.value)
        assertEquals("Daily", viewModel.frequencyType.value)
        assertEquals(medicationId, viewModel.medicationId.value)
    }

    @Test
    fun `test reset all data`() {
        // Given - populate with data
        viewModel.updateMedicationName("Aspirin")
        viewModel.updateDosage("10mg")
        viewModel.updateNotes("Test notes")
        viewModel.updateFrequency("Once Daily")
        viewModel.setSelectedTimes(listOf(LocalTime.of(8, 0)))
        viewModel.setIsEditing(true)

        // When
        viewModel.resetAllData()

        // Then
        assertEquals("", viewModel.medicationName.value)
        assertEquals("", viewModel.dosage.value)
        assertEquals("", viewModel.notes.value)
        assertEquals("", viewModel.frequency.value)
        assertEquals(emptyList<LocalTime>(), viewModel.selectedTimes.value)
        assertEquals(false, viewModel.isEditing.value)
    }
}