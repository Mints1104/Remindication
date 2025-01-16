package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.text.InputType
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
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Fragment responsible for capturing basic health information during user registration.
 * This fragment allows the user to input their first name, last name, and date of birth (DOB).
 * It also handles navigation to the next fragment after the user completes the form.
 */
class HealthInfoFragment : Fragment() {

    private lateinit var viewModel: RegistrationViewModel
    private var datePicker: MaterialDatePicker<Long>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration_part2, container, false)

        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]

        val firstNameEditText = view.findViewById<EditText>(R.id.first_name_edit_text)
        val lastNameEditText = view.findViewById<EditText>(R.id.last_name_edit_text)
        val dobEditText = view.findViewById<EditText>(R.id.dob_edit_text)
        val continueButton = view.findViewById<Button>(R.id.continue_button)

        setupDatePicker(dobEditText)

        continueButton.setOnClickListener {
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val dob = dobEditText.text.toString()

            if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty()) {
                displayMessage(requireView(), "Please fill in all fields.")
                return@setOnClickListener
            }

            if (!viewModel.isAgeValid(dob)) {
                displayMessage(requireView(), "You must be at least 18 years old.")
                return@setOnClickListener
            }

            viewModel.updateRegistrationData {
                this.firstName = firstName
                this.lastName = lastName
                this.dateOfBirth = dob
            }

            findNavController().navigate(R.id.action_healthInfoFragment_to_medicationInfoFragment)
        }

        return view
    }

    private fun setupDatePicker(dobEditText: EditText) {
        // Prevent manual text input
        dobEditText.apply {
            inputType = InputType.TYPE_NULL
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = true
        }

        // Set default date to 18 years ago
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -18)
        val minDate = calendar.timeInMillis

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.UK)

        val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date of Birth")
            .setTheme(R.style.ThemeOverlay_App_DatePicker)
            .setSelection(minDate)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.before(minDate))
                    .build()
            )

        datePicker = datePickerBuilder.build()

        datePicker?.addOnPositiveButtonClickListener { selectedDate ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.timeInMillis = selectedDate

            val dobString = dateFormat.format(selectedCalendar.time)

            if (!viewModel.isAgeValid(dobString)) {
                Toast.makeText(requireContext(), "You must be at least 18 years old", Toast.LENGTH_SHORT).show()
                return@addOnPositiveButtonClickListener
            }

            dobEditText.setText(dobString)
        }

        datePicker?.addOnDismissListener {
            datePicker = null
        }

        dobEditText.setOnClickListener {
            datePicker?.let {
                if (!it.isAdded) {
                    it.show(parentFragmentManager, "DATE_PICKER")
                }
            } ?: run {
                datePicker = datePickerBuilder.build()
                datePicker?.show(parentFragmentManager, "DATE_PICKER")
            }
        }
    }

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        datePicker?.dismiss()
        datePicker = null
    }
}