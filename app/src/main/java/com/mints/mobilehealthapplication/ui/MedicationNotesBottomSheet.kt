package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.BottomsheetMedicationNotesBinding

class MedicationNotesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetMedicationNotesBinding? = null
    private val binding get() = _binding!!
    private var medicationName: String? = null
    private var medicationNotes: String? = null
    private var deviceConnected = false
    private val mainActivity: MainActivity by lazy {
        requireActivity() as MainActivity
    }
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
        if(medicationNotes?.isEmpty() == true) {
            binding.notesTextView.text = "No notes available"
        } else {
            binding.notesTextView.text = medicationNotes
        }

        binding.closeNotesDialogButton.setOnClickListener {
            dismiss()
        }
        deviceConnected = isDeviceConnected()

        binding.btnViewFullDetails.setOnClickListener {
            if(deviceConnected) {
            val bundle = Bundle().apply {
                putString("MEDICATION_NAME", medicationName)
            }
            findNavController().navigate(R.id.global_action_to_medicationInfoFragment, bundle)
            dismiss()
                } else {
                    displayMessage("Device not connected to internet")
            }

        }
    }

    /**
     * Displays a message in a Snackbar at the bottom of the screen.
     */
    private fun displayMessage(msgTxt: String) {
        Snackbar.make(binding.root, msgTxt, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun isDeviceConnected(): Boolean {
        return mainActivity.checkNetworkState()
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
