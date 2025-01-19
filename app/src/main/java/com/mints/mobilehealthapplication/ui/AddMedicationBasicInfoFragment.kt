package com.mints.mobilehealthapplication.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.databinding.FragmentAddMedicationPart1Binding
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel

/**
 * A Fragment to handle adding a medication entry.
 * Allows the user to input medication details such as name, dosage, frequency, reminder time, and notes.
 */
class AddMedicationBasicInfoFragment : Fragment() {

    private var _binding: FragmentAddMedicationPart1Binding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddMedicationViewModel
    private var customFrequencyDialog: AlertDialog? = null
    private var timePickerDialog: TimePickerDialog? = null

    // Selected frequency and a list of standard frequencies
    private var selectedFrequency: String? = null
    private val standardFrequencies = listOf(
        "Once daily",
        "Twice daily",
        "Three times daily",
        "Four times daily",
        "Every morning",
        "Every evening",
        "Every night before bed",
        "As needed",
        "Weekly",
        "Monthly"
    )

    /**
     * Inflates the layout and initializes UI elements for the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using ViewBinding
        _binding = FragmentAddMedicationPart1Binding.inflate(inflater, container, false)
        val view = binding.root



        val mainActivity = activity as MainActivity
        mainActivity.hideFAB()
        mainActivity.hideBottomNav()

        viewModel = ViewModelProvider(this)[AddMedicationViewModel::class.java]

        val medicationNameEditText = binding.medicationNameEditText
        val dosageEditText = binding.dosageEditText
        val notesEditText = binding.notesEditText




        return view
    }






    /**
     * Displays a message in a Snackbar at the bottom of the screen.
     */
    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Called when the view is destroyed, cleans up resources.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("AddMedicationFragment", "onDestroyView called")
        _binding = null
        timePickerDialog?.dismiss()
        timePickerDialog = null
    }
}
