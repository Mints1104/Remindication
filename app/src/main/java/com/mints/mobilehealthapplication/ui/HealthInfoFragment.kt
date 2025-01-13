package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Fragment responsible for capturing basic health information during user registration.
 * This fragment allows the user to input their first name, last name, and date of birth (DOB).
 * It also handles navigation to the next fragment after the user completes the form.
 */
class HealthInfoFragment : Fragment() {

    private lateinit var viewModel: RegistrationViewModel

    /**
     * Called to inflate the fragment's view and initialize necessary components.
     * Binds UI elements, sets up the date picker, validates input, and handles state updates.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_registration_part2, container, false)

        // Initialize ViewModel to share data across fragments
        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]

        // Hide the app bar and bottom navigation for a cleaner registration process
        val mainActivity = requireActivity() as MainActivity
        mainActivity.hideAppBarAndBottomNav()

        // Bind UI elements for first name, last name, and date of birth
        val firstNameEditText = view.findViewById<EditText>(R.id.first_name_edit_text)
        val lastNameEditText = view.findViewById<EditText>(R.id.last_name_edit_text)
        val dobEditText = view.findViewById<EditText>(R.id.dob_edit_text)
        val continueButton = view.findViewById<Button>(R.id.continue_button)

        // Set the default date to 18 years ago as the earliest acceptable age
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -18)

        // Date format for displaying the selected date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Create a date picker for selecting the user's date of birth
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date of Birth")
            .setSelection(calendar.timeInMillis) // Default selection is 18 years ago
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.now()) // Restrict to past dates only
                    .build()
            )
            .build()

        // Set up listener for when a user selects a date from the date picker
        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            // Convert the selected date to the user's local time zone
            val selectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            selectedCalendar.timeInMillis = selectedDate

            val localCalendar = Calendar.getInstance()
            localCalendar.timeInMillis = selectedCalendar.timeInMillis

            // Format the date and update the EditText field
            dobEditText.setText(dateFormat.format(localCalendar.time))
        }

        // Show the date picker when the user clicks on the date of birth field
        dobEditText.setOnClickListener {
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }

        // Set up the Continue button to validate input and navigate to the next fragment
        continueButton.setOnClickListener {
            // Get the input values for first name, last name, and date of birth
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val dob = dobEditText.text.toString()

            // Validate that all fields are filled in
            if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty()) {
                // Show a toast if any field is empty
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update the ViewModel with the user's data
            viewModel.updateRegistrationData {
                this.firstName = firstName
                this.lastName = lastName
                this.dateOfBirth = dob
            }

            // Navigate to the next fragment (MedicationInfoFragment)
            findNavController().navigate(R.id.action_healthInfoFragment_to_medicationInfoFragment)
        }

        return view
    }
}
