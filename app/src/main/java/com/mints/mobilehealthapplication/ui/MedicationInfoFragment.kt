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

/**
 * Fragment responsible for capturing medication information during user registration.
 * This fragment allows the user to input medication details such as medication name, dosage,
 * frequency, and reminder time. It also handles navigation to the next fragment and registration.
 */
class MedicationInfoFragment : Fragment() {

    private lateinit var viewModel: RegistrationViewModel

    /**
     * Called to inflate the fragment's view and initialize necessary components.
     * Binds UI elements, sets up listeners, and manages input validation and state updates.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_registration_part3, container, false)

        // Initialize ViewModel for shared data and state management
        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]

        // Hide app bar and bottom navigation for a cleaner registration process
        val mainActivity = requireActivity() as MainActivity
        mainActivity.hideAppBarAndBottomNav()

        // Bind UI elements to local variables for easier access
        val medicationNameEditText = view.findViewById<EditText>(R.id.medication_name_edit_text)
        val dosageEditText = view.findViewById<EditText>(R.id.dosage_edit_text)
        val frequencyEditText = view.findViewById<EditText>(R.id.frequency_edit_text)
        val reminderTimeEditText = view.findViewById<EditText>(R.id.reminder_time_edit_text)
        val continueButton = view.findViewById<Button>(R.id.continue_button)
        val skipButton = view.findViewById<Button>(R.id.skip_button)

        // Set up TimePicker for the reminder time input
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val timePickerListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            // Set the time based on user selection from the TimePicker
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            reminderTimeEditText.setText(timeFormat.format(calendar.time))
        }

        // Show TimePickerDialog when the user clicks on the reminder time field
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
            // Get input values from the EditText fields
            val medicationName = medicationNameEditText.text.toString()
            val dosage = dosageEditText.text.toString()
            val frequency = frequencyEditText.text.toString()
            val reminderTime = reminderTimeEditText.text.toString()

            // Validate inputs (optional, since the user can skip this step)
            if (medicationName.isEmpty() || dosage.isEmpty() || frequency.isEmpty() || reminderTime.isEmpty()) {
                // Show a toast if any required field is empty
                Toast.makeText(requireContext(), "Please fill in all fields or skip this step", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update the ViewModel with the new registration data
            viewModel.updateRegistrationData {
                this.medicationName = medicationName
                this.dosage = dosage
                this.frequency = frequency
                this.reminderTime = reminderTime
            }

            // Call method to complete the registration process
            completeRegistration()
        }

        // Skip Button Click Listener
        skipButton.setOnClickListener {
            // If the user skips the medication info, complete the registration
            if (findNavController().currentDestination?.id == R.id.medicationInfoFragment) {
                completeRegistration()
            }
        }

        return view
    }

    /**
     * Completes the registration process by calling the ViewModel to register the user
     * and observing the registration state to navigate accordingly.
     */
    private fun completeRegistration() {
        // Call the ViewModel to initiate user registration
        viewModel.registerUser()

        // Observe registration state using StateFlow to handle different states
        lifecycleScope.launch {
            viewModel.registrationState.collect { state ->
                when (state) {
                    is RegistrationViewModel.RegistrationState.Success -> {
                        // If registration is successful, navigate to HomeFragment
                        if (findNavController().currentDestination?.id == R.id.medicationInfoFragment) {
                            findNavController().navigate(R.id.action_medicationInfoFragment_to_homeFragment)
                        }
                    }
                    is RegistrationViewModel.RegistrationState.Error -> {
                        // If there's an error, show a toast with the error message
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    RegistrationViewModel.RegistrationState.Loading -> {
                        // Handle loading state, such as showing a progress indicator (if needed)
                    }
                    RegistrationViewModel.RegistrationState.Initial -> {
                        // Handle initial state (if needed for resetting data or UI)
                    }
                }
            }
        }
    }
}
