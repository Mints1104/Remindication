package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.BottomsheetMedicationNotesBinding

class MedicationNotesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetMedicationNotesBinding? = null
    private val binding get() = _binding!!
    private var medicationName: String? = null
    private var medicationNotes: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetMedicationNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        medicationName = arguments?.getString("MEDICATION_NAME")
        medicationNotes = arguments?.getString("MEDICATION_NOTES")
        Log.d("MedicationNotesBottomSheet", "Medication Name: $medicationName")
        Log.d("MedicationNotesBottomSheet", "Notes: $medicationNotes")
        binding.notesTextView.text = medicationNotes ?: "No notes available"

        binding.closeNotesDialogButton.setOnClickListener {
            dismiss()
        }

        binding.btnViewFullDetails.setOnClickListener {
            val bundle = Bundle().apply {
                putString("MEDICATION_NAME", medicationName)
            }
            findNavController().navigate(R.id.global_action_to_testFragment, bundle)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(name: String, notes: String): MedicationNotesBottomSheet {
            val fragment = MedicationNotesBottomSheet()
            val args = Bundle()
            args.putString("MEDICATION_NAME", name)
            args.putString("MEDICATION_NOTES", notes)
            fragment.arguments = args
            return fragment
        }
    }
}
