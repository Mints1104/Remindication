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

class StreakBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var currentStreakText:TextView
    override fun onCreateView(
        inflater: LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottomsheet_streaks, container, false)
        Log.d("StreakView","We are in streak view.")
        db = Firebase.firestore
        auth = Firebase.auth
        val userId = FirebaseAuth.getInstance().uid

        currentStreakText = view.findViewById(R.id.currentStreakText)
        currentStreakText.text = getString(R.string.current_streak_loading)

        var streak: Int




        val closeButton = view.findViewById<Button>(R.id.closeStreakDialogButton)
        closeButton.setOnClickListener {
            dismiss()
        }

        return view
    }



}
