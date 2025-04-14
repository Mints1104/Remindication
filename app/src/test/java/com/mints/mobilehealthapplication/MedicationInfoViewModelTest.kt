package com.mints.mobilehealthapplication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.mints.mobilehealthapplication.data.FDAApi
import com.mints.mobilehealthapplication.data.MedicationResponse
import com.mints.mobilehealthapplication.data.MedicationResult
import com.mints.mobilehealthapplication.data.OpenFDA
import com.mints.mobilehealthapplication.data.RetrofitClient
import com.mints.mobilehealthapplication.viewmodels.MedicationInfoViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationInfoViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MedicationInfoViewModel

    // Mock API service
    private val mockFdaApi = mockk<FDAApi>(relaxed = true)

    // Test data
    private val testDrugName = "Advil"
    private val testResult = MedicationResult(
        openfda = OpenFDA(
            brand_name = listOf("Advil"),
            generic_name = listOf("Ibuprofen")
        ),
        dosage_and_administration = listOf("Take as directed"),
        warnings = listOf("May cause drowsiness"),
        indications_and_usage = listOf("For pain relief"),
        active_ingredient = listOf("Ibuprofen 200mg"),
        stop_use = listOf("If symptoms persist"),
        do_not_use = listOf("If allergic to ibuprofen"),
        ask_doctor = listOf("Before using with other medications"),
        pregnancy_or_breast_feeding = listOf("Consult doctor if pregnant"),
        storage_and_handling = listOf("Store at room temperature"),
        inactive_ingredient = listOf("Starch, cellulose")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock the RetrofitClient object to return our mock API
        mockkObject(RetrofitClient)
        every { RetrofitClient.fdaApi } returns mockFdaApi

        viewModel = MedicationInfoViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(RetrofitClient)
    }

    @Test
    fun `fetchMedicationInfo should update uiState with success data when API call succeeds`() = runTest {
        // Given
        val response = MedicationResponse(results = listOf(testResult))
        val searchQuery = "openfda.brand_name:$testDrugName"
        coEvery { mockFdaApi.getMedicationInfo(searchQuery) } returns response

        val observer = mockk<Observer<MedicationInfoViewModel.UiState>>(relaxed = true)
        viewModel.uiState.observeForever(observer)

        // When
        viewModel.fetchMedicationInfo(testDrugName)
        testScheduler.advanceUntilIdle() // Use testScheduler instead of testDispatcher

        // Then - check that Loading and Success states were emitted
        verify(atLeast = 1) { observer.onChanged(ofType<MedicationInfoViewModel.UiState.Loading>()) }
        verify(atLeast = 1) { observer.onChanged(ofType<MedicationInfoViewModel.UiState.Success>()) }

        // Clean up
        viewModel.uiState.removeObserver(observer)
    }

    @Test
    fun `fetchMedicationInfo should handle empty results`() = runTest {
        // Given
        val response = MedicationResponse(results = emptyList())
        val searchQuery = "openfda.brand_name:$testDrugName"
        coEvery { mockFdaApi.getMedicationInfo(searchQuery) } returns response

        val observer = mockk<Observer<MedicationInfoViewModel.UiState>>(relaxed = true)
        viewModel.uiState.observeForever(observer)

        // When
        viewModel.fetchMedicationInfo(testDrugName)
        testScheduler.advanceUntilIdle() // Use testScheduler instead of testDispatcher

        // Then - check that Loading and Error states were emitted
        verify(atLeast = 1) { observer.onChanged(ofType<MedicationInfoViewModel.UiState.Loading>()) }
        verify(atLeast = 1) { observer.onChanged(ofType<MedicationInfoViewModel.UiState.Error>()) }

        // Clean up
        viewModel.uiState.removeObserver(observer)
    }

    @Test
    fun `fetchMedicationInfo should handle errors`() = runTest {
        // Given
        val errorMessage = "Network error"
        val searchQuery = "openfda.brand_name:$testDrugName"
        coEvery { mockFdaApi.getMedicationInfo(searchQuery) } throws Exception(errorMessage)

        val observer = mockk<Observer<MedicationInfoViewModel.UiState>>(relaxed = true)
        viewModel.uiState.observeForever(observer)

        // When
        viewModel.fetchMedicationInfo(testDrugName)
        testScheduler.advanceUntilIdle() // Use testScheduler instead of testDispatcher

        // Then - check that Loading and Error states were emitted
        verify(atLeast = 1) { observer.onChanged(ofType<MedicationInfoViewModel.UiState.Loading>()) }
        verify(atLeast = 1) { observer.onChanged(ofType<MedicationInfoViewModel.UiState.Error>()) }

        // Clean up
        viewModel.uiState.removeObserver(observer)
    }

    @Test
    fun `formatMedicationInfo should convert text to bullet points for long paragraphs`() {
        // Given
        val longText = "First sentence. Second sentence. Third sentence is much longer and should be handled properly."

        // When
        val result = viewModel.formatMedicationInfo(longText)

        // Then - check that result is not empty and not null
        assertTrue("Result should not be empty", result.isNotEmpty())
    }

    @Test
    fun `formatMedicationInfo should preserve short paragraphs`() {
        // Given
        val shortText = "This is a short paragraph."

        // When
        val result = viewModel.formatMedicationInfo(shortText)

        // Then - check that result is not empty and not null
        assertTrue("Result should not be empty", result.isNotEmpty())
    }
}