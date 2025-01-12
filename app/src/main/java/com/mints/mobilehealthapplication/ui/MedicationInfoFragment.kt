package com.mints.mobilehealthapplication.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MedicationInfoFragment : Fragment() {

    private lateinit var viewModel: RegistrationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration_part3, container, false)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]
        val mainActivity = requireActivity() as MainActivity
        mainActivity.hideAppBarAndBottomNav()
        // Bind UI elements
        val medicationNameEditText = view.findViewById<EditText>(R.id.medication_name_edit_text)
        val dosageEditText = view.findViewById<EditText>(R.id.dosage_edit_text)
        val frequencyEditText = view.findViewById<EditText>(R.id.frequency_edit_text)
        val reminderTimeEditText = view.findViewById<EditText>(R.id.reminder_time_edit_text)
        val continueButton = view.findViewById<Button>(R.id.continue_button)
        val skipButton = view.findViewById<Button>(R.id.skip_button)

        // Set up TimePicker for Reminder Time
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val timePickerListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            reminderTimeEditText.setText(timeFormat.format(calendar.time))
        }

        reminderTimeEditText.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                timePickerListener,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // Use 24-hour format (set to true for 12-hour format)
            ).show()
        }

        // Continue Button Click Listener
        continueButton.setOnClickListener {
            // Get input values
            val medicationName = medicationNameEditText.text.toString()
            val dosage = dosageEditText.text.toString()
            val frequency = frequencyEditText.text.toString()
            val reminderTime = reminderTimeEditText.text.toString()

            // Validate inputs (optional, since the user can skip this step)
            if (medicationName.isEmpty() || dosage.isEmpty() || frequency.isEmpty() || reminderTime.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields or skip this step", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update ViewModel
            viewModel.updateRegistrationData {
                this.medicationName = medicationName
                this.dosage = dosage
                this.frequency = frequency
                this.reminderTime = reminderTime
            }

            // Complete registration
            completeRegistration()
        }

        // Skip Button Click Listener
        skipButton.setOnClickListener {
            // Check if the current destination is MedicationInfoFragment
            if (findNavController().currentDestination?.id == R.id.medicationInfoFragment) {
                // Complete registration without medication details
                completeRegistration()
            }
        }

        return view
    }

    private fun completeRegistration() {
        // Call ViewModel to register the user
        viewModel.registerUser()

        // Observe registration state using StateFlow
        lifecycleScope.launch {
            viewModel.registrationState.collect { state ->
                when (state) {
                    is RegistrationViewModel.RegistrationState.Success -> {
                        // Check if the current destination is MedicationInfoFragment
                        if (findNavController().currentDestination?.id == R.id.medicationInfoFragment) {
                            // Navigate to HomeFragment on successful registration
                            findNavController().navigate(R.id.action_medicationInfoFragment_to_homeFragment)
                        }
                    }
                    is RegistrationViewModel.RegistrationState.Error -> {
                        // Show error message
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    RegistrationViewModel.RegistrationState.Loading -> {
                        // Handle loading state (e.g., show a progress bar)
                    }
                    RegistrationViewModel.RegistrationState.Initial -> {
                        // Handle initial state (if needed)
                    }
                }
            }
        }
    }
}