// Kotlin
package com.mints.mobilehealthapplication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationHistory
import com.mints.mobilehealthapplication.viewmodels.ProfileViewModel
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@ExperimentalCoroutinesApi
class ProfileViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ProfileViewModel
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockDocRef: DocumentReference
    private lateinit var mockDocSnapshot: DocumentSnapshot
    private lateinit var mockListenerRegistration: ListenerRegistration

    private val testUserId = "test-user-id"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Initialize listener registration
        mockListenerRegistration = mockk(relaxed = true)

        // Mock Firebase Auth
        mockUser = mockk {
            every { uid } returns testUserId
        }
        mockAuth = mockk {
            every { currentUser } returns mockUser
        }

        // Mock DocumentSnapshot and DocumentReference.get()
        mockDocSnapshot = mockk(relaxed = true)
        mockDocRef = mockk {
            every { addSnapshotListener(any<EventListener<DocumentSnapshot>>()) } returns mockListenerRegistration
            every { get() } returns mockk {
                every { addOnSuccessListener(any<OnSuccessListener<DocumentSnapshot>>()) } answers {
                    firstArg<OnSuccessListener<DocumentSnapshot>>().onSuccess(mockDocSnapshot)
                    mockk(relaxed = true)
                }
            }
        }

        // Mock Firestore lookup
        mockFirestore = mockk {
            every { collection(any()) } returns mockk {
                every { document(any()) } returns mockDocRef
            }
        }

        // Stub FireStoreRepository
        mockkObject(FireStoreRepository)

        // Mock Firebase singletons
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockAuth

        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockFirestore

        // Initialize ViewModel
        viewModel = ProfileViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `loadUserProfile fetches and sets user profile correctly`() {
        every { mockDocSnapshot.getString("firstName") } returns "John"
        every { mockDocSnapshot.getString("lastName") } returns "Doe"
        every { mockDocSnapshot.getString("email") } returns "john.doe@example.com"
        every { mockDocSnapshot.getString("dateOfBirth") } returns "1990-01-01"
        every { mockDocSnapshot.getTimestamp("createdAt") } returns mockk {
            every { toDate() } returns Date()
        }

        viewModel.loadUserProfile()

        assertEquals("John", viewModel.userProfile.value?.firstName)
        assertEquals("Doe", viewModel.userProfile.value?.lastName)
        assertEquals("john.doe@example.com", viewModel.userProfile.value?.email)
        assertEquals("1990-01-01", viewModel.userProfile.value?.dateOfBirth)
        assertEquals(testUserId, viewModel.userProfile.value?.uid)
    }

    @Test
    fun `startListeningToAdherenceStreak updates streak value when data changes`() {
        val listenerSlot = slot<EventListener<DocumentSnapshot>>()
        every { mockDocRef.addSnapshotListener(capture(listenerSlot)) } returns mockListenerRegistration
        val mockSnapshot = mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { getLong("adherenceStreak") } returns 7L
        }

        viewModel.startListeningToAdherenceStreak()
        listenerSlot.captured.onEvent(mockSnapshot, null)

        assertEquals(7, viewModel.adherenceStreak.value)
    }

    @Test
    fun `checkMedicationHistory detects when user has taken medication`() = runTest {
        val medicationWithTakenEvent = createMedicationWithEvents(
            listOf(MedicationEvent.Taken(Instant.now()))
        )

        every {
            FireStoreRepository.getMedicationsSnapshot(
                eq(testUserId),
                any<(List<Medication>, Exception?) -> Unit>()
            )
        } answers {
            secondArg<(List<Medication>, Exception?) -> Unit>().invoke(listOf(medicationWithTakenEvent), null)
        }

        viewModel.checkMedicationHistory()

        assertTrue(viewModel.hasEverTakenMedication.value == true)
        assertEquals(1, viewModel.totalDosesTaken.value)
    }

    @Test
    fun `checkMedicationHistory correctly reports when no medications taken`() = runTest {
        val medicationWithNoEvents = createMedicationWithEvents(emptyList())

        every {
            FireStoreRepository.getMedicationsSnapshot(
                eq(testUserId),
                any<(List<Medication>, Exception?) -> Unit>()
            )
        } answers {
            secondArg<(List<Medication>, Exception?) -> Unit>().invoke(listOf(medicationWithNoEvents), null)
        }

        viewModel.checkMedicationHistory()

        assertFalse(viewModel.hasEverTakenMedication.value == true)
        assertEquals(0, viewModel.totalDosesTaken.value)
    }

    @Test
    fun `calculatePerfectWeeks counts weeks with all taken events`() = runTest {
        val thisWeek = LocalDate.now()
        val thisWeekDateTime = LocalDateTime.of(thisWeek.year, thisWeek.month, thisWeek.dayOfMonth, 10, 0)
        val thisWeekInstant1 = thisWeekDateTime.atZone(ZoneId.systemDefault()).toInstant()
        val thisWeekInstant2 = thisWeekDateTime.plusDays(1).atZone(ZoneId.systemDefault()).toInstant()
        val lastWeek = thisWeek.minusWeeks(1)
        val lastWeekDateTime = LocalDateTime.of(lastWeek.year, lastWeek.month, lastWeek.dayOfMonth, 10, 0)
        val lastWeekInstant1 = lastWeekDateTime.atZone(ZoneId.systemDefault()).toInstant()
        val lastWeekInstant2 = lastWeekDateTime.plusDays(1).atZone(ZoneId.systemDefault()).toInstant()

        val medication1 = createMedicationWithEvents(listOf(
            MedicationEvent.Taken(thisWeekInstant1),
            MedicationEvent.Taken(lastWeekInstant1)
        ))
        val medication2 = createMedicationWithEvents(listOf(
            MedicationEvent.Taken(thisWeekInstant2),
            MedicationEvent.Skipped(lastWeekInstant2)
        ))

        every {
            FireStoreRepository.getMedicationsSnapshot(
                eq(testUserId),
                any<(List<Medication>, Exception?) -> Unit>()
            )
        } answers {
            secondArg<(List<Medication>, Exception?) -> Unit>().invoke(listOf(medication1, medication2), null)
        }

        viewModel.calculatePerfectWeeks()

        // Only this week is perfect since last week contains a Skipped event.
        assertEquals(1, viewModel.perfectWeeks.value)
    }

    @Test
    fun `getMedications fetches medications correctly`() = runTest {
        val medication1 = createMedicationWithEvents(emptyList())
        val medication2 = createMedicationWithEvents(emptyList())
        val medications = listOf(medication1, medication2)

        every {
            FireStoreRepository.getMedicationsSnapshot(
                eq(testUserId),
                any<(List<Medication>, Exception?) -> Unit>()
            )
        } answers {
            secondArg<(List<Medication>, Exception?) -> Unit>().invoke(medications, null)
        }

        var completionCalled = false

        viewModel.getMedications(testUserId) {
            completionCalled = true
        }

        assertEquals(medications, viewModel.medications.value)
        assertTrue(completionCalled)
    }

    // Helper function to create a Medication with a given event list.
    // Kotlin
    private fun createMedicationWithEvents(events: List<MedicationEvent>): Medication {
        // Convert the immutable list to a mutable list.
        val history = MedicationHistory(events.toMutableList())
        return mockk {
            every { id } returns "med-${events.size}"
            every { medicationHistory } returns history
        }
    }
}