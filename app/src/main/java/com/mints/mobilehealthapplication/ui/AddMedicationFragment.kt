package com.mints.mobilehealthapplication.ui

import android.R
import android.os.Bundle
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

        val mainActivity = activity as MainActivity
        mainActivity.hideFAB()
        mainActivity.hideBottomNav()

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MedicationViewModel::class.java]

        // Set up UI elements
        val medicationNameEditText = binding.medicationNameEditText
        val dosageEditText = binding.dosageEditText
        val frequencySpinner = binding.frequencySpinner
        val frequencyOptions = listOf("Once a day", "Twice a day", "Three times a day", "As needed")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, frequencyOptions)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        frequencySpinner.adapter = adapter

        val timePicker = binding.timePicker
        val notesEditText = binding.notesEditText

        // Set up save button click listener
        binding.saveMedicationButton.setOnClickListener {
            val medicationName = medicationNameEditText.text.toString().trim()
            val dosage = dosageEditText.text.toString().trim()
             val frequency = frequencySpinner.selectedItem.toString()
            val time = "${timePicker.hour}:${timePicker.minute}"
            val notes = notesEditText.text.toString().trim()

            if (medicationName.isEmpty() || dosage.isEmpty()) {
                Toast.makeText(context, "Name and dosage are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val medication = Medication(medicationName, dosage, frequency, time, notes)
            viewModel.saveMedication(FirebaseAuth.getInstance().uid ?: "", medication)
        }

        // Observe save result LiveData
        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Medication saved successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(context, "Failed to save medication", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up cancel button click listener
        binding.closeAddMedicationView.setOnClickListener {
            findNavController().navigateUp()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}