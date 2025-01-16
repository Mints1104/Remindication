package com.mints.mobilehealthapplication.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
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
        val view = inflater.inflate(R.layout.fragment_registration_part3, container, false)
        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]

        val medicationNameEditText = view.findViewById<EditText>(R.id.medication_name_edit_text)
        val dosageEditText = view.findViewById<EditText>(R.id.dosage_edit_text)
        val frequencyChipGroup = view.findViewById<ChipGroup>(R.id.frequency_chip_group)
        val reminderTimeEditText = view.findViewById<EditText>(R.id.reminder_time_edit_text)
        val continueButton = view.findViewById<Button>(R.id.continue_button)
        val skipButton = view.findViewById<Button>(R.id.skip_button)

        setupFrequencyChips(frequencyChipGroup)
        setupTimePicker(reminderTimeEditText)

        // Restore values from the ViewModel
        medicationNameEditText.setText(viewModel.registrationData.value.medicationName)
        dosageEditText.setText(viewModel.registrationData.value.dosage)
        reminderTimeEditText.setText(viewModel.registrationData.value.reminderTime)
        getSelectedFrequency(frequencyChipGroup)

        // Save values to the ViewModel as they change
        medicationNameEditText.doAfterTextChanged {
            viewModel.registrationData.value.medicationName = it?.toString() ?: ""
        }
        dosageEditText.doAfterTextChanged {
            viewModel.registrationData.value.dosage = it?.toString() ?: ""
        }
        reminderTimeEditText.doAfterTextChanged {
            viewModel.registrationData.value.reminderTime = it?.toString() ?: ""
        }
        frequencyChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedChip = group.findViewById<Chip>(checkedIds[0])
                viewModel.registrationData.value.frequency = selectedChip.text.toString()
            }
        }

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

        return view
    }

    private fun setupTimePicker(reminderTimeEditText: EditText) {
        reminderTimeEditText.apply {
            inputType = InputType.TYPE_NULL
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = true
            hint = getString(R.string.reminder_time_hint)
        }

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

        reminderTimeEditText.setOnClickListener {
            timePickerDialog?.dismiss()
            timePickerDialog = getTimePickerDialog().also { it.show() }
        }

        reminderTimeEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                timePickerDialog?.dismiss()
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
        standardFrequencies.forEach { frequency ->
            val chip = createChip(frequency)
            chipGroup.addView(chip)
        }

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

    private fun getSelectedFrequency(chipGroup: ChipGroup): String {
        val frequency = viewModel.registrationData.value.frequency

        if (frequency.isNotEmpty()) {
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip
                if (chip?.text?.toString() == frequency) {
                    chipGroup.check(chip.id) // Restore the selected chip
                    break
                }
            }
        }

        val selectedId = chipGroup.checkedChipId
        return if (selectedId != View.NO_ID) {
            chipGroup.findViewById<Chip>(selectedId)?.text?.toString() ?: ""
        } else ""
    }

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
                    RegistrationViewModel.RegistrationState.Loading -> {}
                    RegistrationViewModel.RegistrationState.Initial -> {}
                }
            }
        }
    }

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timePickerDialog?.dismiss()
        timePickerDialog = null
    }
}