package com.mints.mobilehealthapplication.ui

import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment responsible for capturing medication information during user registration.
 * This fragment allows the user to input medication details such as medication name, dosage,
 * frequency, and reminder time. It also handles navigation to the next fragment and registration.
 */
class MedicationInfoFragment : Fragment() {

    private lateinit var viewModel: RegistrationViewModel
    private var timePickerDialog: TimePickerDialog? = null
    private var customFrequencyDialog: AlertDialog? = null


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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(com.mints.mobilehealthapplication.R.layout.fragment_registration_part3, container, false)

        // Initialize ViewModel for shared data and state management
        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]

        // Bind UI elements to local variables for easier access
        val medicationNameEditText = view.findViewById<EditText>(R.id.medication_name_edit_text)
        val dosageEditText = view.findViewById<EditText>(R.id.dosage_edit_text)
        val frequencyChipGroup = view.findViewById<ChipGroup>(R.id.frequency_chip_group)
        val reminderTimeEditText = view.findViewById<EditText>(R.id.reminder_time_edit_text)
        val continueButton = view.findViewById<Button>(R.id.continue_button)
        val skipButton = view.findViewById<Button>(R.id.skip_button)

        setupFrequencyChips(frequencyChipGroup)
        setupTimePicker(reminderTimeEditText)

        // Continue Button Click Listener
        continueButton.setOnClickListener {
            // Get input values from the EditText fields
            val medicationName = medicationNameEditText.text.toString()
            val dosage = dosageEditText.text.toString()
            val frequency = getSelectedFrequency(frequencyChipGroup)
            val reminderTime = reminderTimeEditText.text.toString()

            // Validate inputs (optional, since the user can skip this step)
            if (medicationName.isEmpty() || dosage.isEmpty() || frequency.isEmpty() || reminderTime.isEmpty()) {
                displayMessage(requireView(),getString(R.string.fill_in_all_fields_p3))
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

    private fun setupTimePicker(reminderTimeEditText: EditText) {
        // Prevent manual text input
        reminderTimeEditText.apply {
            inputType = InputType.TYPE_NULL
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = true

            // Set initial hint
            hint = getString(R.string.reminder_time_hint)
        }

        // Initialize TimePickerDialog lazily
        val getTimePickerDialog = {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    val formattedTime = formatTime(hour, minute)
                    reminderTimeEditText.setText(formattedTime)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false  // 12-hour format
            ).apply {
                // Ensure dialog can't be dismissed without selection
                setCancelable(true)
                setOnCancelListener {
                    // If no time was previously set, clear the field
                    if (reminderTimeEditText.text.isEmpty()) {
                        reminderTimeEditText.setText("")
                    }
                }
            }
        }

        // Show time picker on both click and focus
        reminderTimeEditText.setOnClickListener {
            timePickerDialog?.dismiss()  // Dismiss any existing dialog
            timePickerDialog = getTimePickerDialog().also { it.show() }
        }

        // Handle focus events
        reminderTimeEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                timePickerDialog?.dismiss()  // Dismiss any existing dialog
                timePickerDialog = getTimePickerDialog().also { it.show() }
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val formattedHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (hour >= 12) "PM" else "AM"
        return String.format("%d:%02d %s", formattedHour, minute, amPm)
    }

    private fun setupFrequencyChips(chipGroup: ChipGroup) {
        // Add standard frequency options
        standardFrequencies.forEach { frequency ->
            val chip = createChip(frequency)
            chipGroup.addView(chip)
        }

        // Add "Custom" chip as the last option
        val customChip = createChip("Custom...")
        customChip.setOnClickListener {
            showCustomFrequencyDialog(chipGroup)
        }
        chipGroup.addView(customChip)
    }

    private fun createChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_state_colors)

            // Optional: Set text color to be white when selected, default text color when not
            setTextColor(ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(
                    ContextCompat.getColor(requireContext(), android.R.color.white),
                    ContextCompat.getColor(requireContext(), R.color.black)
                )
            ))
        }
    }

    private fun showCustomFrequencyDialog(chipGroup: ChipGroup) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_frequency, null)
        val editText = dialogView.findViewById<EditText>(R.id.custom_frequency_edit_text)

        customFrequencyDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enter Custom Frequency")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val customFrequency = editText.text.toString()
                if (customFrequency.isNotEmpty()) {
                    // Remove the custom chip if it exists
                    chipGroup.findViewWithTag<Chip>("custom")?.let {
                        chipGroup.removeView(it)
                    }
                    // Add the new custom frequency
                    val chip = createChip(customFrequency).apply {
                        tag = "custom"
                        isChecked = true
                    }
                    chipGroup.addView(chip, chipGroup.childCount - 1) // Add before the "Custom..." chip
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        customFrequencyDialog?.show()
    }

    private fun getSelectedFrequency(chipGroup: ChipGroup): String {
        val selectedId = chipGroup.checkedChipId
        return if (selectedId != View.NO_ID) {
            chipGroup.findViewById<Chip>(selectedId)?.text?.toString() ?: ""
        } else ""
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
                        displayMessage(requireView(),state.message)
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

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up dialog to prevent window leaks
        timePickerDialog?.dismiss()
        timePickerDialog = null
    }
}