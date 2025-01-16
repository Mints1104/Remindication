package com.mints.mobilehealthapplication.ui

import android.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.databinding.FragmentAddMedicationBinding
import com.mints.mobilehealthapplication.viewmodels.MedicationViewModel

class AddMedicationFragment : Fragment() {

    private var _binding: FragmentAddMedicationBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MedicationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMedicationBinding.inflate(inflater, container, false)
        val view = binding.root

        Log.d("AddMedicationFragment", "onCreateView called")

        val mainActivity = activity as MainActivity
        mainActivity.hideFAB()
        mainActivity.hideBottomNav()

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MedicationViewModel::class.java]
        Log.d("AddMedicationFragment", "ViewModel initialized")

        // Set up UI elements
        val medicationNameEditText = binding.medicationNameEditText
        val dosageEditText = binding.dosageEditText
        val frequencySpinner = binding.frequencySpinner
        val frequencyOptions = listOf("Once a day", "Twice a day", "Three times a day", "As needed")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, frequencyOptions)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        frequencySpinner.adapter = adapter
        Log.d("AddMedicationFragment", "Frequency spinner initialized with options: $frequencyOptions")

        val timePicker = binding.timePicker
        val notesEditText = binding.notesEditText

        // Set up save button click listener
        binding.saveMedicationButton.setOnClickListener {
            Log.d("AddMedicationFragment", "Save button clicked")

            val medicationName = medicationNameEditText.text.toString().trim()
            val dosage = dosageEditText.text.toString().trim()
            val frequency = frequencySpinner.selectedItem.toString()
            val time = "${timePicker.hour}:${timePicker.minute}"
            val notes = notesEditText.text.toString().trim()
            val id = ""

            Log.d("AddMedicationFragment", "Medication details entered:")
            Log.d("AddMedicationFragment", "Name: $medicationName")
            Log.d("AddMedicationFragment", "Dosage: $dosage")
            Log.d("AddMedicationFragment", "Frequency: $frequency")
            Log.d("AddMedicationFragment", "Time: $time")
            Log.d("AddMedicationFragment", "Notes: $notes")

            if (medicationName.isEmpty() || dosage.isEmpty()) {
                Log.d("AddMedicationFragment", "Validation failed: Name or dosage is empty")
                Toast.makeText(context, "Name and dosage are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val medication = Medication(
                name = medicationName,
                dosage = dosage,
                frequency = frequency,
                time = time,
                notes = notes
            )
            Log.d("AddMedicationFragment", "Medication object created: $medication")

            val uid = FirebaseAuth.getInstance().uid ?: ""
            Log.d("AddMedicationFragment", "Current user UID: $uid")

            if (uid.isEmpty()) {
                Log.e("AddMedicationFragment", "User is not authenticated")
                Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveMedication(uid, medication)
        }

        // Observe save result LiveData
        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Log.d("AddMedicationFragment", "Medication saved successfully")
                Toast.makeText(context, "Medication saved successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Log.e("AddMedicationFragment", "Failed to save medication")
                Toast.makeText(context, "Failed to save medication", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up cancel button click listener
        binding.closeAddMedicationView.setOnClickListener {
            Log.d("AddMedicationFragment", "Cancel button clicked")
            findNavController().navigateUp()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("AddMedicationFragment", "onDestroyView called")
        _binding = null
    }
}