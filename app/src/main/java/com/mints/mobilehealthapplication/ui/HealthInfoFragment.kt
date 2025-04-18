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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.FragmentRegistrationPart2Binding
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import kotlinx.coroutines.launch
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
    ): View {
        _binding = FragmentRegistrationPart2Binding.inflate(inflater, container, false)
        val view = binding.root
        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]
        setUpUI()
        handleRegisterUser()
        return view
    }


    private fun setUpUI() {
        setupDatePicker(binding.dobEditText)
        binding.firstNameEditText.setText(viewModel.registrationData.value.firstName)
        binding.lastNameEditText.setText(viewModel.registrationData.value.lastName)
        binding.dobEditText.setText(viewModel.registrationData.value.dateOfBirth)
    }


    private fun handleRegisterUser() {
        binding.completeRegistration.setOnClickListener {
            val firstName = binding.firstNameEditText.text.toString()
            val lastName = binding.lastNameEditText.text.toString()
            val dob = binding.dobEditText.text.toString()
            if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty()) {
                displayMessage("Please fill in all fields.")
                return@setOnClickListener
            }
            if (!viewModel.isAgeValid(dob)) {
                displayMessage("You must be at least 18 years old.")
                return@setOnClickListener
            }
            viewModel.updateRegistrationData {
                this.firstName = firstName
                this.lastName = lastName
                this.dateOfBirth = dob
            }
            completeRegistration()
        }
    }


    /**
     * Completes the registration process and navigates to the next screen.
     */
    private fun completeRegistration() {
        viewModel.registerUser()
        lifecycleScope.launch {
            viewModel.registrationState.collect { state ->
                when (state) {
                    is RegistrationViewModel.RegistrationState.Success -> {
                        if (findNavController().currentDestination?.id == R.id.healthInfoFragment) {
                            viewModel.resetRegistrationData()
                            findNavController().navigate(R.id.action_healthInfoFragment_to_homeFragment)
                        }
                    }
                    is RegistrationViewModel.RegistrationState.Error -> {
                        displayMessage(state.message)
                    }
                    RegistrationViewModel.RegistrationState.Loading -> {
                        // Optionally show loading indicator
                    }
                    RegistrationViewModel.RegistrationState.Initial -> {
                    }
                }
            }
        }
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

        dobEditText.setOnClickListener {
            // Create a new date picker instance every time
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
                viewModel.updateRegistrationData {
                    this.dateOfBirth = dobString
                }
            }

            datePicker?.addOnDismissListener {
                datePicker = null
            }

            datePicker?.show(parentFragmentManager, "DATE_PICKER")
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

    override fun onPause() {
        super.onPause()
        //Save current data before leaving the fragment
        val firstName = binding.firstNameEditText.text.toString()
        val lastName = binding.lastNameEditText.text.toString()
        val dob = binding.dobEditText.text.toString()

        viewModel.updateRegistrationData {
            this.firstName = firstName
            this.lastName = lastName
            this.dateOfBirth = dob
        }
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
