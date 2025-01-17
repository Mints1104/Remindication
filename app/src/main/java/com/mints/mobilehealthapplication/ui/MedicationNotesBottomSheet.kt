package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.databinding.BottomsheetMedicationNotesBinding

class MedicationNotesBottomSheet : BottomSheetDialogFragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var notes: String? = null

    private var _binding: BottomsheetMedicationNotesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetMedicationNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("MedicationNotesBottomSheet", "We are in medication notes bottom sheet.")
        db = Firebase.firestore
        auth = Firebase.auth

        notes = arguments?.getString("NOTES_KEY")
        Log.d("MedicationNotesBottomSheet", "Notes: $notes")
        binding.notesTextView.text = notes ?: "No notes available"

        binding.closeNotesDialogButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(notes: String): MedicationNotesBottomSheet {
            val fragment = MedicationNotesBottomSheet()
            val args = Bundle()
            args.putString("NOTES_KEY", notes)
            fragment.arguments = args
            return fragment
        }
    }
}