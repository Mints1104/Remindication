package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.BottomsheetStreaksBinding
import java.time.LocalDate
import java.time.ZoneId

/**
 * A BottomSheetFragment that displays a user's streak data.
 * This fragment connects to Firebase Firestore to fetch and update the user's login streak.
 */
class StreakBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var _binding: BottomsheetStreaksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetStreaksBinding.inflate(inflater, container, false)
        Log.d("StreakView", "We are in streak view.")

        db = Firebase.firestore
        auth = Firebase.auth

        // Show loading text while we fetch/update streak data
        binding.currentStreakText.text = getString(R.string.current_streak_loading)

        // Set up close button for the bottom sheet
        binding.closeStreakDialogButton.setOnClickListener {
            dismiss()
        }

        // If the user is logged in, update the login streak
        val userId = FirebaseAuth.getInstance().uid
        if (userId != null) {
            updateLoginStreak(userId)
        } else {
            Log.e("Streak", "User is not logged in")
        }

        return binding.root
    }

    /**
     * Retrieves the current streak data from Firestore, calculates the new streak,
     * and then saves the updated data back to Firestore.
     *
     * The logic:
     * 1. If there's no document or no lastLogin date, start with streak = 1.
     * 2. If lastLogin is today, do nothing.
     * 3. If lastLogin was yesterday, increment the streak.
     * 4. Otherwise, reset the streak to 1.
     */
    private fun updateLoginStreak(userId: String) {
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            val currentDate = LocalDate.now()
            var newStreak = 1 // default if no document or not consecutive

            if (document.exists()) {
                // Get current streak and last login date from Firestore
                val loginStreak = document.getLong("loginStreak")?.toInt() ?: 0
                val lastLoginTimestamp = document.getTimestamp("lastLogin")
                if (lastLoginTimestamp != null) {
                    // Convert Firestore Timestamp to LocalDate
                    val lastLoginDate = lastLoginTimestamp.toDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                    when {
                        lastLoginDate.isEqual(currentDate) -> {
                            // Already logged in today; no change needed.
                            newStreak = loginStreak
                        }
                        lastLoginDate.plusDays(1).isEqual(currentDate) -> {
                            // Last login was yesterday; increment streak.
                            newStreak = loginStreak + 1
                        }
                        else -> {
                            // Gap in days; reset streak.
                            newStreak = 1
                        }
                    }
                }
            }

            // Prepare data to update Firestore with the new streak and current login date
            val data = hashMapOf(
                "loginStreak" to newStreak,
                "lastLogin" to Timestamp.now()
            )

            userDocRef.set(data, SetOptions.merge())
                .addOnSuccessListener {
                    // Update the UI text with the new streak value
                    binding.currentStreakText.text = "Current Streak: $newStreak"
                }
                .addOnFailureListener { e ->
                    Log.e("Streak", "Error updating login streak", e)
                }
        }.addOnFailureListener { exception ->
            Log.e("Streak", "Error retrieving user document", exception)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
