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

class HealthInfoFragment : Fragment() {

    private lateinit var viewModel: RegistrationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration_part2, container, false)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]
        val mainActivity = requireActivity() as MainActivity
        mainActivity.hideAppBarAndBottomNav()

        // Bind UI elements
        val firstNameEditText = view.findViewById<EditText>(R.id.first_name_edit_text)
        val lastNameEditText = view.findViewById<EditText>(R.id.last_name_edit_text)
        val dobEditText = view.findViewById<EditText>(R.id.dob_edit_text)
        val continueButton = view.findViewById<Button>(R.id.continue_button)

        // Set the default date to 18 years ago
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -18)

        // Date format for display
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Create a date picker builder
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date of Birth")
            .setSelection(calendar.timeInMillis) // Default selection (18 years ago)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.now()) // Restrict to past dates
                    .build()
            )
            .build()

        // Set up the date picker listener
        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val selectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            selectedCalendar.timeInMillis = selectedDate

            // Convert to local time zone
            val localCalendar = Calendar.getInstance()
            localCalendar.timeInMillis = selectedCalendar.timeInMillis

            dobEditText.setText(dateFormat.format(localCalendar.time))
        }

        // Show the date picker when the EditText is clicked
        dobEditText.setOnClickListener {
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }

        // Continue Button Click Listener
        continueButton.setOnClickListener {
            // Get input values
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val dob = dobEditText.text.toString()

            // Validate inputs
            if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update ViewModel
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