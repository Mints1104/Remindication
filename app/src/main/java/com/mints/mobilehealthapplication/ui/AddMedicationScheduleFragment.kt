package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.ScheduleValidator
import com.mints.mobilehealthapplication.databinding.FragmentAddMedicationPart3ScheduleBinding
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AddMedicationScheduleFragment : Fragment() {

    private var _binding: FragmentAddMedicationPart3ScheduleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddMedicationViewModel by activityViewModels()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val tag = "M.ScheduleFragment"
    private var userId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMedicationPart3ScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        Log.d(tag,"In MedicationScheduleFragment...")
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    private fun setupViews() {
        // Set up back button
        (activity as? MainActivity)?.apply {
            hideFAB()
            hideBottomNav()
        }

        resetContainerVisibility()
        setContainerVisibility()
        handleIsEditing()
        setUpTimePicker()
        setUpDaySelection()
    }

    private fun handleIsEditing() {
        if (viewModel.getIsEditing() == true) {
            displayMessage("We are editing")
            val test = viewModel.getFrequency()

            Log.d(tag, "Get Frequency: $test")
            setUpUpdateButton()

            if (viewModel.getFrequencyType() == "Weekly") {
                val frequencyString = viewModel.getFrequency() ?: ""

                val selectedDays = frequencyString.split(",")
                    .map { it.trim() }
                    .mapNotNull { abbreviation ->
                        when (abbreviation) {
                            "Mon" -> R.id.mondayChip
                            "Tue" -> R.id.tuesdayChip
                            "Wed" -> R.id.wednesdayChip
                            "Thu" -> R.id.thursdayChip
                            "Fri" -> R.id.fridayChip
                            "Sat" -> R.id.saturdayChip
                            "Sun" -> R.id.sundayChip
                            else -> null
                        }
                    }

                selectedDays.forEach { chipId ->
                    binding.daysChipGroup.findViewById<Chip>(chipId)?.isChecked = true
                }
            }

        } else {
            displayMessage("We are not editing")

            setUpSaveButton()
        }
    }

    private fun setUpTimePicker() {
        listOf(
            binding.dailyTimeInput,
            binding.weeklyTimeInput,
            binding.cyclicTimeInput,
            binding.firstTimeInput,
            binding.secondTimeInput
        ).forEach { editText ->
            editText.setOnClickListener {
                showTimePicker(editText)
            }
        }
    }

    private fun setUpDaySelection() {
        binding.daysChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val days = mutableSetOf<DayOfWeek>()
            checkedIds.forEach { chipId ->
                when (chipId) {
                    R.id.mondayChip -> days.add(DayOfWeek.MONDAY)
                    R.id.tuesdayChip -> days.add(DayOfWeek.TUESDAY)
                    R.id.wednesdayChip -> days.add(DayOfWeek.WEDNESDAY)
                    R.id.thursdayChip -> days.add(DayOfWeek.THURSDAY)
                    R.id.fridayChip -> days.add(DayOfWeek.FRIDAY)
                    R.id.saturdayChip -> days.add(DayOfWeek.SATURDAY)
                    R.id.sundayChip -> days.add(DayOfWeek.SUNDAY)
                }
            }
            viewModel.setSelectedDays(days)
        }
    }

    private fun setUpUpdateButton() {

        binding.saveButton.text = getString(R.string.update_txt)
        binding.saveButton.setOnClickListener {
            when (viewModel.getFrequencyType()) {
                "Once Daily", "Twice Daily" -> {
                    if (validateDailySchedule()) {
                        Log.d(tag, "Attempting to update medication daily")
                        updateMedication(userId)
                    }
                }

                "Weekly" -> {
                    if (validateWeeklySchedule()) {
                        Log.d(tag, "Attempting to update medication weekly")

                        updateMedication(userId)
                    } else {
                        Log.d(tag, "Failed  to update medication weekly")

                    }
                }

                "Cyclic" -> {
                    if (validateCyclicSchedule()) {
                        Log.d(tag, "Attempting to update medication cyclic")

                        updateMedication(userId)
                    }
                }

                "On Demand" -> {
                    if (validateOnDemand()) {
                        Log.d(tag, "Attempting to update medication on demand")

                        updateMedication(userId)
                    }
                }

                else -> showError("Invalid schedule type")
            }
        }
    }

    private fun setUpSaveButton() {
        binding.saveButton.setOnClickListener {
            when (viewModel.getFrequencyType()) {
                "Once Daily", "Twice Daily" -> {
                    if (validateDailySchedule()) {
                        Log.d(tag, "Attempting to save medication daily")
                        saveMedication()
                    }
                }

                "Weekly" -> {
                    if (validateWeeklySchedule()) {
                        Log.d(tag, "Attempting to save medication weekly")

                        saveMedication()
                    } else {
                        Log.d(tag, "Failed  to save medication weekly")

                    }
                }

                "Cyclic" -> {
                    if (validateCyclicSchedule()) {
                        Log.d(tag, "Attempting to save medication cyclic")

                        saveMedication()
                    }
                }

                "On Demand" -> {
                    if (validateOnDemand()) {
                        Log.d(tag, "Attempting to save medication on demand")

                        saveMedication()
                    }
                }

                else -> showError("Invalid schedule type")
            }
        }
    }

    private fun setupObservers() {


        viewModel.validationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AddMedicationViewModel.ValidationState.Invalid -> {
                    Log.d(tag,"Invalid state")

                    showError(state.message)
                    viewModel.resetValidationState()
                    Log.d(tag,"Resetting validation state!")

                }
                AddMedicationViewModel.ValidationState.Valid -> {
                    Log.d(tag,"Validation state -> valid")

                }
                AddMedicationViewModel.ValidationState.Initial -> {
                    Log.d(tag,"Validation state -> Initial")

                }
            }
        }
    }

    private fun showTimePicker(editText: TextInputEditText) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setTitleText("Select Time")
            .build()

        picker.addOnPositiveButtonClickListener {
            val time = LocalTime.of(picker.hour, picker.minute)
            editText.setText(time.format(timeFormatter))

        }

        picker.show(parentFragmentManager, "TIME_PICKER_${editText.id}")
    }

    private fun setContainerVisibility() {
        when (viewModel.getFrequencyType()) {
            "Once Daily" -> binding.dailyScheduleContainer.isVisible = true
            "Weekly" -> binding.weeklyScheduleContainer.isVisible = true
            "Cyclic" -> binding.cyclicScheduleContainer.isVisible = true
            "On demand" -> binding.onDemandContainer.isVisible = true
            "Twice Daily" -> binding.twiceDailyScheduleContainer.isVisible = true
            else -> showError("Unknown schedule type")
        }
    }

    private fun resetContainerVisibility() {
        Log.d(tag,"Resetting visibility")

        binding.dailyScheduleContainer.isVisible = false
        binding.weeklyScheduleContainer.isVisible = false
        binding.cyclicScheduleContainer.isVisible = false
        binding.onDemandContainer.isVisible = false
        binding.twiceDailyScheduleContainer.isVisible = false
    }

    private fun validateDailySchedule(): Boolean {
        return when (viewModel.getFrequency()) {
            "Once Daily" -> validateOnceDaily()
            "Twice Daily" -> validateTwiceDaily()
            else -> {
                showError("Invalid daily schedule type")
                false
            }
        }
    }

    private fun validateOnceDaily(): Boolean {
        val time = parseTime(binding.dailyTimeInput.text.toString())
        return if (time != null) {
            viewModel.setSelectedTimes(listOf(time))
            viewModel.setWithFoodStatus(binding.withFoodSwitch.isChecked)
            true
        } else {
            showError("Please select a time")
            false
        }
    }

    private fun validateTwiceDaily(): Boolean {
        val firstTime = parseTime(binding.firstTimeInput.text.toString())
        val secondTime = parseTime(binding.secondTimeInput.text.toString())

        return when {
            firstTime == null || secondTime == null -> {
                showError("Both times required")
                false
            }
            firstTime == secondTime -> {
                showError("Times must be different")
                false
            }
            firstTime.isAfter(secondTime) -> {
                showError("Second time must be after first")
                false
            }
            else -> {
                viewModel.setSelectedTimes(listOf(firstTime, secondTime))
                viewModel.setWithFoodStatus(binding.twiceDailyWithFoodSwitch.isChecked)
                true
            }
        }
    }

    private fun parseTime(input: String): LocalTime? {
        return try {
            LocalTime.parse(input, timeFormatter)
        } catch (e: Exception) {
            null
        }
    }

    private fun validateWeeklySchedule(): Boolean {
        val times = parseTimes(binding.weeklyTimeInput.text.toString())
        return if (times.isNotEmpty() && viewModel.selectedDays.value?.isNotEmpty() == true) {
            viewModel.setSelectedTimes(times)
            viewModel.validateSchedule()
        } else {
            showError("Please select days and time")
            false
        }
    }

    private fun validateCyclicSchedule(): Boolean {
        val intakeDays = binding.intakeDaysInput.text.toString().toIntOrNull()
        val pauseDays = binding.pauseDaysInput.text.toString().toIntOrNull()
        val times = parseTimes(binding.cyclicTimeInput.text.toString())

        return if (intakeDays != null && pauseDays != null && times.isNotEmpty()) {
            viewModel.updateIntakeDays(intakeDays)
            viewModel.updatePauseDays(pauseDays)
            viewModel.setSelectedTimes(times)
            viewModel.validateSchedule()
        } else {
            showError("Please fill all fields")
            false
        }
    }

    private fun validateOnDemand(): Boolean {
        val maxDoses = binding.maxDosesInput.text.toString().toIntOrNull()
        val minHours = binding.minHoursBetweenInput.text.toString().toIntOrNull()
        return if (ScheduleValidator.isValidOnDemandSchedule(maxDoses, minHours)) {
            viewModel.updateMaxDoses(maxDoses)
            viewModel.updateMinHoursBetween(minHours)
            viewModel.validateSchedule()
        } else {
            showError("Invalid on-demand parameters")
            false
        }
    }

    private fun parseTimes(input: String): List<LocalTime> {
        return input.split(",")
            .map { it.trim() }
            .mapNotNull {
                try {
                    LocalTime.parse(it, timeFormatter)
                } catch (e: Exception) {
                    null
                }
            }
    }

    private fun saveMedication() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            showError("User not authenticated")
            return
        }
        Log.d(tag,"Saving medication!")

        viewModel.saveMedication(userId)

        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Log.d(tag,"Successfully saved medication")
                displayMessage("Successfully saved medication")

                navigateToNextFragment()
            } else {
                Log.d(tag,"Failed to save medication")
                showError("Failed to save medication")
            }
        }
    }

    private fun updateMedication(userId: String) {


        viewModel.updateMedication(userId)

        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Log.d(tag,"Successfully updated medication")
                displayMessage("Successfully updated medication")

                navigateToNextFragment()
            } else {
                Log.d(tag,"Failed to updated medication")
                displayMessage("Failed to  update medication")

                showError("Failed to updated medication")
            }
        }
    }

    private fun navigateToNextFragment() {
        if (isAdded && !isStateSaved) {
            findNavController().navigate(
                R.id.action_addMedicationScheduleFragment_to_homeFragment
            )
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

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(tag,"In on destroy...")

        _binding = null
    }
}