package com.mints.mobilehealthapplication.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.databinding.FragmentAddMedicationBinding
import com.mints.mobilehealthapplication.viewmodels.MedicationViewModel
import java.util.Calendar
import java.util.Locale

/**
 * A Fragment to handle adding a medication entry.
 * Allows the user to input medication details such as name, dosage, frequency, reminder time, and notes.
 */
class AddMedicationFragment : Fragment() {

    private var _binding: FragmentAddMedicationBinding? = null
    private val binding get() = _binding!! // Safe reference to the binding object

    private lateinit var viewModel: MedicationViewModel
    private var customFrequencyDialog: AlertDialog? = null
    private var timePickerDialog: TimePickerDialog? = null

    // Selected frequency and a list of standard frequencies
    private var selectedFrequency: String? = null
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

    /**
     * Inflates the layout and initializes UI elements for the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using ViewBinding
        _binding = FragmentAddMedicationBinding.inflate(inflater, container, false)
        val view = binding.root

        Log.d("AddMedicationFragment", "onCreateView called")

        // Set up the frequency chip group UI element
        val frequencyChipGroup = view.findViewById<ChipGroup>(R.id.frequency_chip_group)

        // Hide FAB and bottom navigation from the parent activity
        val mainActivity = activity as MainActivity
        mainActivity.hideFAB()
        mainActivity.hideBottomNav()

        // Initialize ViewModel to handle medication data
        viewModel = ViewModelProvider(this)[MedicationViewModel::class.java]
        Log.d("AddMedicationFragment", "ViewModel initialized")

        // Set up UI elements for user input
        val medicationNameEditText = binding.medicationNameEditText
        val dosageEditText = binding.dosageEditText
        setupFrequencyChips(frequencyChipGroup)  // Set up frequency chips
        val reminderTimeEditText = binding.reminderTimeEditText

        // Set up time picker dialog for reminder time input
        setupTimePicker(reminderTimeEditText)

        val notesEditText = binding.notesEditText

        // Set up the save button to save medication details
        binding.saveMedicationButton.setOnClickListener {
            Log.d("AddMedicationFragment", "Save button clicked")

            // Collect medication details from user input
            val medicationName = medicationNameEditText.text.toString().trim()
            val dosage = dosageEditText.text.toString().trim()
            val reminderTime = reminderTimeEditText.text.toString()
            val notes = notesEditText.text.toString().trim()

            // Log the entered medication details
            Log.d("AddMedicationFragment", "Medication details entered:")
            Log.d("AddMedicationFragment", "Name: $medicationName")
            Log.d("AddMedicationFragment", "Dosage: $dosage")
            Log.d("AddMedicationFragment", "Frequency: $selectedFrequency")
            Log.d("AddMedicationFragment", "Time: $reminderTime")
            Log.d("AddMedicationFragment", "Notes: $notes")

            // Validate required fields
            if (medicationName.isEmpty() || dosage.isEmpty()) {
                Log.d("AddMedicationFragment", "Validation failed: Name or dosage is empty")
                displayMessage(requireView(), getString(R.string.name_and_dosage_required))
                return@setOnClickListener
            }

            // Create a medication object to save
            val medication = Medication(
                name = medicationName,
                dosage = dosage,
                frequency = selectedFrequency ?: return@setOnClickListener,
                time = reminderTime,
                notes = notes
            )
            Log.d("AddMedicationFragment", "Medication object created: $medication")

            // Retrieve the current authenticated user's UID
            val uid = FirebaseAuth.getInstance().uid ?: ""
            Log.d("AddMedicationFragment", "Current user UID: $uid")

            // Check if user is authenticated
            if (uid.isEmpty()) {
                Log.e("AddMedicationFragment", "User is not authenticated")
                displayMessage(requireView(), getString(R.string.user_not_authenticated))
                return@setOnClickListener
            }

            // Save the medication to the database using ViewModel
            viewModel.saveMedication(uid, medication)
        }

        // Observe result of saving medication and handle success/failure
        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Log.d("AddMedicationFragment", "Medication saved successfully")
                displayMessage(requireView(), getString(R.string.medication_saved_successfully))
                findNavController().navigateUp()  // Navigate back to previous screen
            } else {
                Log.e("AddMedicationFragment", "Failed to save medication")
                displayMessage(requireView(), getString(R.string.failed_to_save_medication))
            }
        }

        return view  // Return the root view
    }

    /**
     * Sets up the time picker dialog for selecting reminder time.
     */
    private fun setupTimePicker(reminderTimeEditText: EditText) {
        // Set up properties for the reminder time input field
        reminderTimeEditText.apply {
            inputType = InputType.TYPE_NULL
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = true
            hint = getString(R.string.reminder_time_hint)
        }

        // Create a time picker dialog
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

        // Show time picker when clicked
        reminderTimeEditText.setOnClickListener {
            timePickerDialog?.dismiss()
            timePickerDialog = getTimePickerDialog().also { it.show() }
        }

        // Show time picker when focused
        reminderTimeEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                timePickerDialog?.dismiss()
                timePickerDialog = getTimePickerDialog().also { it.show() }
            }
        }
    }

    /**
     * Formats the given hour and minute into a 12-hour format with AM/PM.
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
     * Sets up the frequency chip group with predefined frequency options.
     */
    private fun setupFrequencyChips(chipGroup: ChipGroup) {
        standardFrequencies.forEach { frequency ->
            val chip = createChip(frequency)
            chipGroup.addView(chip)
        }

        // Add a custom chip to allow user input for a custom frequency
        val customChip = createChip("Custom...")
        customChip.setOnClickListener {
            showCustomFrequencyDialog(chipGroup)  // Show dialog for custom frequency
        }
        chipGroup.addView(customChip)
    }

    /**
     * Creates a chip for the given frequency text.
     */
    private fun createChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedFrequency = text  // Set the selected frequency when checked
                }
            }
        }
    }

    /**
     * Shows the dialog to input a custom frequency.
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
                        chipGroup.removeView(it)  // Remove existing custom chip
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
     * Displays a message in a Snackbar at the bottom of the screen.
     */
    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Called when the view is destroyed, cleans up resources.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("AddMedicationFragment", "onDestroyView called")
        _binding = null
        timePickerDialog?.dismiss()
        timePickerDialog = null
    }
}
