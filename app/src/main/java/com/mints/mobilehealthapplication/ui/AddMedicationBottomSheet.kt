package com.mints.mobilehealthapplication.ui


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R

class AddMedicationBottomSheet : BottomSheetDialogFragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottomsheet_addmedication, container, false)
        Log.d("AddMedicationView","We are in add medication bottom sheet")
        db = Firebase.firestore
        auth = Firebase.auth
        val userId = FirebaseAuth.getInstance().uid






        val closeButton = view.findViewById<Button>(R.id.closeAddMedicationView)
        closeButton.setOnClickListener {
            dismiss()
        }

        return view
    }



}
