package com.mints.mobilehealthapplication.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.UserProfile
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

class ProfileViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> get() = _userProfile

    // Add streak tracking
    private val _adherenceStreak = MutableLiveData<Int>(0)
    val adherenceStreak: LiveData<Int> = _adherenceStreak

    // Track if user has ever taken medication
    private val _hasEverTakenMedication = MutableLiveData<Boolean>(false)
    val hasEverTakenMedication: LiveData<Boolean> = _hasEverTakenMedication

    private val _totalDosesTaken = MutableLiveData<Int>(0)
    val totalDosesTaken: LiveData<Int> = _totalDosesTaken

    private val _perfectWeeks = MutableLiveData<Int>(0)
    val perfectWeeks: LiveData<Int> = _perfectWeeks

    private val _medications = MutableLiveData<List<Medication>>()
    val medications: LiveData<List<Medication>> get() = _medications

    private var streakListener: ListenerRegistration? = null

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = UserProfile(
                    firstName = document.getString("firstName") ?: "",
                    lastName = document.getString("lastName") ?: "",
                    email = document.getString("email") ?: "",
                    dateOfBirth = document.getString("dateOfBirth") ?: "",
                    createdAt = document.getTimestamp("createdAt")?.toDate()?.toString() ?: "",
                    uid = userId
                )
                _userProfile.value = user
            }
    }

    fun calculatePerfectWeeks() {
        val userId = auth.currentUser?.uid ?: return

        FireStoreRepository.getMedicationsSnapshot(userId) { medications, error ->
            if (error != null) {
                Log.e("ProfileViewModel", "Error loading medications", error)
                return@getMedicationsSnapshot
            }

            val eventsByWeek = mutableMapOf<Int, MutableList<MedicationEvent>>()

            medications.forEach { medication ->
                medication.medicationHistory.events.forEach { event ->
                    // Convert Instant to LocalDate using the system default zone.
                    val localDate = event.date.atZone(ZoneId.systemDefault()).toLocalDate()
                    val weekFields = WeekFields.of(Locale.getDefault())
                    val weekNumber = localDate.get(weekFields.weekOfYear())
                    val year = localDate.year
                    val weekKey = year * 100 + weekNumber
                    eventsByWeek.getOrPut(weekKey) { mutableListOf() }.add(event)
                }
            }

            val perfectWeeksCount = eventsByWeek.count { (_, events) ->
                events.all { it is MedicationEvent.Taken }
            }

            _perfectWeeks.value = perfectWeeksCount
            Log.d("PerfectWeeks", "User has $perfectWeeksCount perfect weeks")
        }
    }

    fun startListeningToAdherenceStreak() {
        val userId = auth.currentUser?.uid ?: return
        streakListener?.remove()
        val userDocRef = firestore.collection("users").document(userId)
        streakListener = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AdherenceListener", "Listen failed", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val streak = snapshot.getLong("adherenceStreak")?.toInt() ?: 0
                _adherenceStreak.value = streak
                Log.d("AdherenceListener", "Current adherence streak: $streak")
            }
        }
    }

    fun checkMedicationHistory() {
        val userId = auth.currentUser?.uid ?: return
        FireStoreRepository.getMedicationsSnapshot(userId) { medications, error ->
            if (error != null) {
                Log.e("ProfileViewModel", "Error loading medications", error)
                return@getMedicationsSnapshot
            }
            val hasTakenAny = medications.any { medication ->
                medication.medicationHistory.events.any { it is MedicationEvent.Taken }
            }
            val totalDosesTaken = medications.sumOf { medication ->
                medication.medicationHistory.getEventCount(MedicationEvent.EventType.TAKEN)
            }
            _totalDosesTaken.value = totalDosesTaken
            _hasEverTakenMedication.value = hasTakenAny
            Log.d("MedicationHistory", "User has taken medication before: $hasTakenAny")
            Log.d("MedicationHistory", "Total doses taken: $totalDosesTaken")
        }
    }

    fun getTotalDosesTaken(): Int {
        return _totalDosesTaken.value ?: 0
    }

    fun getMedications(uid: String, onComplete: () -> Unit = {}) {
        Log.d("HomeFragmentViewModel", "Starting real-time medication listener for user: $uid")

        FireStoreRepository.getMedicationsSnapshot(uid) { meds, error ->
            if (error != null) {
                Log.e("HomeFragmentVM", "Failed to get medications: ${error.message}")
                onComplete() // Even on error, signal completion
                return@getMedicationsSnapshot
            }

            Log.d("HomeFragmentViewModel", "Fetched ${meds.size} medications from snapshot")
            _medications.postValue(meds) // Update LiveData with the new list
            onComplete()
        }
    }


    override fun onCleared() {
        super.onCleared()
        // Clean up listener when ViewModel is destroyed
        streakListener?.remove()
    }
}