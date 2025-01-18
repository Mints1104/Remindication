package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.mints.mobilehealthapplication.databinding.FragmentRegistrationPart2Binding
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
    private var _binding: FragmentRegistrationPart2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegistrationPart2Binding.inflate(inflater, container, false)
        val view = binding.root

        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]

        setupDatePicker(binding.dobEditText)

        binding.firstNameEditText.setText(viewModel.registrationData.value.firstName)
        binding.lastNameEditText.setText(viewModel.registrationData.value.lastName)
        binding.dobEditText.setText(viewModel.registrationData.value.dateOfBirth)

        binding.continueButton.setOnClickListener {
            val firstName = binding.firstNameEditText.text.toString()
            val lastName = binding.lastNameEditText.text.toString()
            val dob = binding.dobEditText.text.toString()

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
        datePicker?.let { picker ->
            if (picker.isAdded) {
                picker.dismiss()
            }
        }
        datePicker = null
        _binding = null
    }
}
