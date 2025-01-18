package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.BottomsheetStreaksBinding

/**
 * A BottomSheetFragment that displays a user's streak data.
 * This fragment connects to Firebase Firestore to fetch the user's current streak.
 */
class StreakBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var _binding: BottomsheetStreaksBinding? = null
    private val binding get() = _binding!!

    /**
     * Inflates the view and sets up UI components for displaying streak data.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetStreaksBinding.inflate(inflater, container, false)

        Log.d("StreakView", "We are in streak view.")

        db = Firebase.firestore
        auth = Firebase.auth
        val userId = FirebaseAuth.getInstance().uid
        val currentStreakText: TextView = binding.currentStreakText
        currentStreakText.text = getString(R.string.current_streak_loading)
        var streak: Int
        val closeButton: Button = binding.closeStreakDialogButton
        closeButton.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    /**
     * Called when the fragment is destroyed to clean up the ViewBinding reference.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
