package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.BottomsheetStreaksBinding


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
        Log.d("StreakView", "We are in adherence streak view.")

        db = Firebase.firestore
        auth = Firebase.auth

        binding.currentStreakText.text = getString(R.string.current_streak_loading)

        binding.closeStreakDialogButton.setOnClickListener {
            dismiss()
        }

        val userId = FirebaseAuth.getInstance().uid
        if (userId != null) {
            listenToAdherenceChanges(userId)

        } else {
            Log.e("Streak", "User is not logged in")
        }

        return binding.root
    }



    private fun listenToAdherenceChanges(userId: String) {
        val userDocRef = db.collection("users").document(userId)
        userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AdherenceListener", "Listen failed", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists() && _binding != null) {
                val streak = snapshot.getLong("adherenceStreak")?.toInt() ?: 0
                _binding?.currentStreakText?.text = "Current Adherence Streak: $streak"
                Log.d("AdherenceListener", "Current adherence streak: $streak")
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
