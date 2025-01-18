package com.mints.mobilehealthapplication.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.FragmentRegistrationPart3Binding
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Fragment for collecting medication information in the registration flow.
 */
class MedicationInfoFragment : Fragment() {

    private val viewModel: RegistrationViewModel by activityViewModels()
    private var timePickerDialog: TimePickerDialog? = null
    private var customFrequencyDialog: AlertDialog? = null
    private var _binding: FragmentRegistrationPart3Binding? = null
    private val binding get() = _binding!!
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
        _binding = FragmentRegistrationPart3Binding.inflate(inflater, container, false)

        val medicationNameEditText = binding.medicationNameEditText
        val dosageEditText = binding.dosageEditText
        val frequencyChipGroup = binding.frequencyChipGroup
        val reminderTimeEditText = binding.reminderTimeEditText
        val continueButton = binding.continueButton
        val skipButton = binding.skipButton

        setupFrequencyChips(frequencyChipGroup)
        setupTimePicker(reminderTimeEditText)

        // Restore values from the ViewModel, if available
        medicationNameEditText.setText(viewModel.registrationData.value.medicationName)
        dosageEditText.setText(viewModel.registrationData.value.dosage)
        reminderTimeEditText.setText(viewModel.registrationData.value.reminderTime)
        getSelectedFrequency(frequencyChipGroup)

        // Save input values to ViewModel when they change
        medicationNameEditText.doAfterTextChanged {
            viewModel.registrationData.value.medicationName = it?.toString() ?: ""
        }
        dosageEditText.doAfterTextChanged {
            viewModel.registrationData.value.dosage = it?.toString() ?: ""
        }
        reminderTimeEditText.doAfterTextChanged {
            viewModel.registrationData.value.reminderTime = it?.toString() ?: ""
        }

        // Handle frequency selection changes
        frequencyChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedChip = group.findViewById<Chip>(checkedIds[0])
                viewModel.registrationData.value.frequency = selectedChip.text.toString()
            }
        }

        // Continue button: Validate input and complete registration
        continueButton.setOnClickListener {
            val medicationName = medicationNameEditText.text.toString()
            val dosage = dosageEditText.text.toString()
            val frequency = getSelectedFrequency(frequencyChipGroup)
            val reminderTime = reminderTimeEditText.text.toString()

            if (medicationName.isEmpty() || dosage.isEmpty() || frequency.isEmpty() || reminderTime.isEmpty()) {
                displayMessage(requireView(), getString(R.string.fill_in_all_fields_p3))
                return@setOnClickListener
            }

            completeRegistration()
        }

        skipButton.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.medicationInfoFragment) {
                completeRegistration()
            }
        }

        return binding.root
    }

    /**
     * Initializes the time picker for setting a reminder time.
     */
    private fun setupTimePicker(reminderTimeEditText: EditText) {
        reminderTimeEditText.apply {
            inputType = InputType.TYPE_NULL
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = true
            hint = getString(R.string.reminder_time_hint)
        }

        // Create time picker dialog
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
                false
            ).apply {
                setCancelable(true)
                setOnCancelListener {
                    if (reminderTimeEditText.text.isEmpty()) {
                        reminderTimeEditText.setText("")
                    }
                }
            }
        }

        // Show time picker when the user clicks on the input field
        reminderTimeEditText.setOnClickListener {
            timePickerDialog?.dismiss()
            timePickerDialog = getTimePickerDialog().also { it.show() }
        }

        // Show time picker when the input field gains focus
        reminderTimeEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                timePickerDialog?.dismiss()
                timePickerDialog = getTimePickerDialog().also { it.show() }
            }
        }
    }

    /**
     * Formats the time in 12-hour format (e.g., "1:30 PM").
     */
    private fun formatTime(hour: Int, minute: Int): String {
        val formattedHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (hour >= 12) "PM" else "AM"
        return String.format(Locale.getDefault(), "%d:%02d %s", formattedHour, minute, amPm)
    }

    /**
     * Sets up frequency chip buttons for standard frequencies and custom input.
     */
    private fun setupFrequencyChips(chipGroup: ChipGroup) {
        standardFrequencies.forEach { frequency ->
            val chip = createChip(frequency)
            chipGroup.addView(chip)
        }

        // Add a custom frequency chip with an option to enter a custom value
        val customChip = createChip("Custom...")
        customChip.setOnClickListener {
            showCustomFrequencyDialog(chipGroup)
        }
        chipGroup.addView(customChip)
    }

    /**
     * Creates a chip with the given text.
     */
    private fun createChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
        }
    }

    /**
     * Displays a dialog to enter a custom frequency.
     */
    private fun showCustomFrequencyDialog(chipGroup: ChipGroup) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_frequency, null)
        val editText = dialogView.findViewById<EditText>(R.id.custom_frequency_edit_text)

        customFrequencyDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enter Custom Frequency")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val customFrequency = editText.text.toString()
                if (customFrequency.isNotEmpty()) {
                    chipGroup.findViewWithTag<Chip>("custom")?.let {
                        chipGroup.removeView(it)
                    }
                    val chip = createChip(customFrequency).apply {
                        tag = "custom"
                        isChecked = true
                    }
                    chipGroup.addView(chip, chipGroup.childCount - 1)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        customFrequencyDialog?.show()
    }

    /**
     * Retrieves the selected frequency from the chip group.
     */
    private fun getSelectedFrequency(chipGroup: ChipGroup): String {
        val frequency = viewModel.registrationData.value.frequency

        // Restore previously selected frequency if available
        if (frequency.isNotEmpty()) {
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip
                if (chip?.text?.toString() == frequency) {
                    chipGroup.check(chip.id) // Restore the selected chip
                    break
                }
            }
        }

        // Return the selected frequency or an empty string if none is selected
        val selectedId = chipGroup.checkedChipId
        return if (selectedId != View.NO_ID) {
            chipGroup.findViewById<Chip>(selectedId)?.text?.toString() ?: ""
        } else ""
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
                        if (findNavController().currentDestination?.id == R.id.medicationInfoFragment) {
                            findNavController().navigate(R.id.action_medicationInfoFragment_to_homeFragment)
                        }
                    }
                    is RegistrationViewModel.RegistrationState.Error -> {
                        displayMessage(requireView(), state.message)
                    }
                    RegistrationViewModel.RegistrationState.Loading -> {
                        // Optionally show loading indicator
                    }
                    RegistrationViewModel.RegistrationState.Initial -> {
                        // Initial state, no action needed
                    }
                }
            }
        }
    }

    /**
     * Displays a message as a Snackbar.
     */
    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timePickerDialog?.dismiss()
        timePickerDialog = null
        _binding = null
    }
}
