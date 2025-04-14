package com.mints.mobilehealthapplication

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel.ErrorType
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel.RegistrationState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: RegistrationViewModel
    private val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    private val mockAuthTask = mockk<com.google.android.gms.tasks.Task<AuthResult>>(relaxed = true)
    private val mockAuthResult = mockk<AuthResult>(relaxed = true)
    private val mockUser = mockk<FirebaseUser>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns mockAuthTask

        mockkObject(FireStoreRepository)

        viewModel = RegistrationViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `isAgeValid should return true if user is 18 or older`() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -20)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dob = dateFormat.format(calendar.time)

        val result = viewModel.isAgeValid(dob)

        assertTrue(result)
    }

    @Test
    fun `isAgeValid should return false if user is under 18`() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -17)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dob = dateFormat.format(calendar.time)

        val result = viewModel.isAgeValid(dob)

        assertFalse(result)
    }

    @Test
    fun `isAgeValid should return false for invalid date format`() {
        val result = viewModel.isAgeValid("abc/def/ghij")
        assertFalse(result)
    }

    @Test
    fun `updateRegistrationData should update fields`() = runTest {
        viewModel.updateRegistrationData {
            firstName = "John"
            lastName = "Doe"
            email = "john@example.com"
            password = "Password123"
            dateOfBirth = "01/01/1990"
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("John", viewModel.registrationData.value.firstName)
        assertEquals("Doe", viewModel.registrationData.value.lastName)
        assertEquals("john@example.com", viewModel.registrationData.value.email)
        assertEquals("Password123", viewModel.registrationData.value.password)
        assertEquals("01/01/1990", viewModel.registrationData.value.dateOfBirth)
    }

    @Test
    fun `registerUser fails if under 18`() = runTest {
        viewModel.updateRegistrationData {
            firstName = "John"
            lastName = "Doe"
            email = "john@example.com"
            password = "Password123"

            // Set underage date
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, -17)
            dateOfBirth = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
        }

        viewModel.registerUser()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.registrationState.value
        assertTrue(state is RegistrationState.Error)
        assertEquals(ErrorType.AGE_RESTRICTION, (state as RegistrationState.Error).type)
    }

    @Test
    fun `registerUser succeeds with valid data`() = runTest {
        viewModel.updateRegistrationData {
            firstName = "John"
            lastName = "Doe"
            email = "john@example.com"
            password = "Password123"
            dateOfBirth = "01/01/1990"
        }

        // Mock Task behavior
        every { mockAuthTask.isSuccessful } returns true
        every { mockAuthTask.result } returns mockAuthResult
        every { mockAuthResult.user } returns mockUser
        every { mockUser.uid } returns "test-user-123"

        // Most important line - mock the kotlinx.coroutines.tasks.await extension function
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { mockAuthTask.await() } returns mockAuthResult

        coEvery { FireStoreRepository.saveUserData("test-user-123", any()) } returns true

        viewModel.registerUser()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.registrationState.value
        assertTrue(state is RegistrationState.Success)
    }

    @Test
    fun `registerUser fails if email already in use`() = runTest {
        viewModel.updateRegistrationData {
            firstName = "John"
            lastName = "Doe"
            email = "existing@example.com"
            password = "Password123"
            dateOfBirth = "01/01/1990"
        }

        val exception = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "Email already in use")

        // Mock the await() call to throw the exception
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { mockAuthTask.await() } throws exception

        // Collect states
        val states = mutableListOf<RegistrationState>()
        val job = launch {
            viewModel.registrationState.collect { states.add(it) }
        }

        viewModel.registerUser()
        testDispatcher.scheduler.advanceUntilIdle()

        job.cancel()
        assertTrue(states.any { it is RegistrationState.Error && it.type == ErrorType.EMAIL_EXISTS })
    }

    @Test
    fun `registerUser fails with weak password`() = runTest {
        viewModel.updateRegistrationData {
            firstName = "John"
            lastName = "Doe"
            email = "john@example.com"
            password = "weak"
            dateOfBirth = "01/01/1990"
        }

        val exception = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "Weak password", null)

        // Mock the await() call to throw the exception
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { mockAuthTask.await() } throws exception

        // State collection
        val states = mutableListOf<RegistrationState>()
        val job = launch {
            viewModel.registrationState.collect { states.add(it) }
        }

        viewModel.registerUser()

        testDispatcher.scheduler.advanceUntilIdle()

        job.cancel()
        assertTrue(states.any { it is RegistrationState.Error && it.type == ErrorType.WEAK_PASSWORD })
    }

    @Test
    fun `registerUser fails with invalid email`() = runTest {
        viewModel.updateRegistrationData {
            firstName = "John"
            lastName = "Doe"
            email = "invalid-email"
            password = "Password123"
            dateOfBirth = "01/01/1990"
        }

        val exception = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "Invalid email")

        // Mock the await() call to throw the exception
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { mockAuthTask.await() } throws exception

        // Collect states
        val states = mutableListOf<RegistrationState>()
        val job = launch {
            viewModel.registrationState.collect { states.add(it) }
        }

        viewModel.registerUser()
        testDispatcher.scheduler.advanceUntilIdle()

        job.cancel()
        assertTrue(states.any { it is RegistrationState.Error && it.type == ErrorType.INVALID_EMAIL })
    }

    @Test
    fun `registerUser fails if user data not saved`() = runTest {
        viewModel.updateRegistrationData {
            firstName = "John"
            lastName = "Doe"
            email = "john@example.com"
            password = "Password123"
            dateOfBirth = "01/01/1990"
        }

        // Mock successful auth but failed database save
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { mockAuthTask.await() } returns mockAuthResult
        every { mockAuthResult.user } returns mockUser
        every { mockUser.uid } returns "test-user-123"

        coEvery { FireStoreRepository.saveUserData("test-user-123", any()) } returns false

        // Collect states
        val states = mutableListOf<RegistrationState>()
        val job = launch {
            viewModel.registrationState.collect { states.add(it) }
        }

        viewModel.registerUser()
        testDispatcher.scheduler.advanceUntilIdle()

        job.cancel()
        assertTrue(states.any { it is RegistrationState.Error && it.type == ErrorType.GENERAL })
    }
}