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
import androidx.navigation.navOptions
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.application.MedicationApp
import com.mints.mobilehealthapplication.data.ScheduleValidator
import com.mints.mobilehealthapplication.databinding.FragmentAddMedicationPart3ScheduleBinding
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AddMedicationScheduleFragment : Fragment() {

    private var _binding: FragmentAddMedicationPart3ScheduleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddMedicationViewModel by activityViewModels()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val tag = "ScheduleFragment"
    private var userId = ""
    private var deviceConnected = false
    private val mainActivity: MainActivity by lazy {
        requireActivity() as MainActivity
    }

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
        Log.d(tag,"Get frequency: ${viewModel.getFrequency()}")

        Log.d(tag,"Get frequencyType: ${viewModel.getFrequencyType()}")
        deviceConnected = isDeviceConnected()
        /*
        observeNetworkState()


        if(!deviceConnected) {
            displayMessage("Internet connection lost")
            findNavController().navigate(R.id.action_addMedicationBasicInfoFragment_to_homeFragment)
        } else {
            Log.d(tag, "Device is connected to the internet")
        }

         */

    }

    private fun observeNetworkState() {
        mainActivity.internetChecker.connectionState.observe(viewLifecycleOwner) { isConnected ->
            if (!isConnected) {
                displayMessage("Internet connection lost")
                if (findNavController().currentDestination?.id == R.id.addMedicationBasicInfoFragment) {
                    findNavController().navigate(R.id.action_addMedicationBasicInfoFragment_to_homeFragment)
                }
            }
        }
    }

    private fun isDeviceConnected(): Boolean {
        return mainActivity.checkNetworkState()
    }

    private fun setupViews() {
      mainActivity.apply {
          hideFAB()
          hideBottomNav()
      }
        resetContainerVisibility()
        setContainerVisibility()
        handleIsEditing()
        setupTextWatchers()

        populateFieldsFromViewModel()
        setUpTimePicker()
        setUpDaySelection()
    }

    private fun handleIsEditing() {
        if (viewModel.getIsEditing() == true) {
            val test = viewModel.getFrequency()
            Log.d(tag, "Get Frequency: $test")
            setUpUpdateButton()
        } else {
            setUpSaveButton()
        }
    }


    private fun setupTextWatchers() {
        binding.intakeDaysInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                s?.toString()?.toIntOrNull()?.let { days ->
                    viewModel.updateIntakeDays(days)
                }
            }
        })

        binding.pauseDaysInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                s?.toString()?.toIntOrNull()?.let { days ->
                    viewModel.updatePauseDays(days)
                }
            }
        })
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
                    } else {
                        Log.d(tag, "Failed to update medication daily")
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
                    } else {
                        Log.d(tag, "Failed to update medication cyclic")
                    }
                }
                "On Demand" -> {
                    if (validateOnDemand()) {
                        Log.d(tag, "Attempting to update medication on demand")
                        updateMedication(userId)
                    } else {
                        Log.d(tag, "Failed to update medication on demand")
                    }
                }
                else -> displayMessage("Invalid schedule type")
            }
        }
    }


    private fun setUpSaveButton() {
        Log.d(tag,"Setting up save button")

        binding.saveButton.setOnClickListener {
            when (viewModel.getFrequencyType()) {
                "Once Daily", "Twice Daily" -> {
                    if (validateDailySchedule()) {
                        Log.d(tag, "Attempting to save medication daily")
                        saveMedication(userId)
                    }
                }
                "Weekly" -> {
                    if (validateWeeklySchedule()) {
                        Log.d(tag, "Attempting to save medication weekly")
                        saveMedication(userId)
                    } else {
                        Log.d(tag, "Failed  to save medication weekly")
                    }
                }

                "Cyclic" -> {
                    if (validateCyclicSchedule()) {
                        Log.d(tag, "Attempting to save medication cyclic")
                        saveMedication(userId)
                    }
                }
                "On Demand" -> {
                    if (validateOnDemand()) {
                        Log.d(tag, "Attempting to save medication on demand")
                        saveMedication(userId)
                    }
                }
                else -> displayMessage("Invalid schedule type")
            }
        }
    }


    private fun setupObservers() {
        viewModel.validationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AddMedicationViewModel.ValidationState.Invalid -> {
                    Log.d(tag,"Invalid state")
                    displayMessage(state.message)
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
        Log.d(tag,"Showing time picker")
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setTitleText("Select Time")
            .setTheme(R.style.ThemeOverlay_App_TimePicker)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .build()
        picker.addOnPositiveButtonClickListener {
            val time = LocalTime.of(picker.hour, picker.minute)
            editText.setText(time.format(timeFormatter))

            when (editText.id) {
                R.id.dailyTimeInput -> viewModel.setSelectedTimes(listOf(time))
                R.id.weeklyTimeInput -> {
                    val currentTimes = viewModel.getSelectedTimes() ?: emptyList()
                    if (currentTimes.isEmpty()) {
                        viewModel.setSelectedTimes(listOf(time))
                    } else {
                        viewModel.setSelectedTimes(listOf(time))
                    }
                }
                R.id.cyclicTimeInput -> viewModel.setSelectedTimes(listOf(time))
                R.id.firstTimeInput -> {
                    val secondTime = viewModel.getSelectedTimes()?.getOrNull(1)
                    if (secondTime != null) {
                        viewModel.setSelectedTimes(listOf(time, secondTime))
                    } else {
                        viewModel.setSelectedTimes(listOf(time))
                    }
                }
                R.id.secondTimeInput -> {
                    val firstTime = viewModel.getSelectedTimes()?.firstOrNull()
                    if (firstTime != null) {
                        viewModel.setSelectedTimes(listOf(firstTime, time))
                    } else {
                        viewModel.setSelectedTimes(listOf(LocalTime.now(), time))
                    }
                }
            }
        }
        picker.show(parentFragmentManager, "TIME_PICKER_${editText.id}")
    }


    private fun setContainerVisibility() {
        Log.d(tag,"Setting container visibility: ${viewModel.getFrequencyType()}")

        when (viewModel.getFrequencyType()) {
            "Once Daily" -> binding.dailyScheduleContainer.isVisible = true
            "Weekly" -> binding.weeklyScheduleContainer.isVisible = true
            "Cyclic" -> binding.cyclicScheduleContainer.isVisible = true
            "On Demand" -> binding.onDemandContainer.isVisible = true
            "Twice Daily" -> binding.twiceDailyScheduleContainer.isVisible = true
            else -> displayMessage("Unknown schedule type")
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
                displayMessage("Invalid daily schedule type")
                false
            }
        }
    }


    private fun validateOnceDaily(): Boolean {
        val time = parseTime(binding.dailyTimeInput.text.toString())
        return if (time != null) {
            viewModel.setSelectedTimes(listOf(time))
            true
        } else {
            displayMessage("Please select a time")
            false
        }
    }


    private fun validateTwiceDaily(): Boolean {
        val firstTime = parseTime(binding.firstTimeInput.text.toString())
        val secondTime = parseTime(binding.secondTimeInput.text.toString())
        return when {
            firstTime == null || secondTime == null -> {
                displayMessage("Both times required")
                false
            }
            firstTime == secondTime -> {
                displayMessage("Times must be different")
                false
            }
            firstTime.isAfter(secondTime) -> {
                displayMessage("Second time must be after first")
                false
            }
            else -> {
                viewModel.setSelectedTimes(listOf(firstTime, secondTime))
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
        val timeText = binding.weeklyTimeInput.text.toString()
        val times = parseTimes(timeText)
        val selectedDays = viewModel.selectedDays.value

        Log.d(tag, "Weekly validation - Time text: '$timeText', Parsed times: ${times.size}, Selected days: ${selectedDays?.size}")

        return if (times.isNotEmpty() && selectedDays?.isNotEmpty() == true) {
            viewModel.setSelectedTimes(times)
            viewModel.validateSchedule()
        } else {
            displayMessage("Please select days and time")
            Log.d(tag, "Weekly validation failed - times empty: ${times.isEmpty()}, days empty: ${selectedDays?.isEmpty()}")
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
            displayMessage("Please fill all fields")
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
            displayMessage("Invalid on-demand parameters")
            false
        }
    }

    private fun populateFieldsFromViewModel() {
        val frequencyType = viewModel.getFrequencyType()
        Log.d(tag, "Populating fields for frequency type: $frequencyType")

        val times = viewModel.getSelectedTimes()

        when (frequencyType) {
            "Once Daily" -> {
                times?.firstOrNull()?.let { time ->
                    binding.dailyTimeInput.setText(time.format(timeFormatter))
                }
            }
            "Twice Daily" -> {
                if (times != null && times.size >= 2) {
                    binding.firstTimeInput.setText(times[0].format(timeFormatter))
                    binding.secondTimeInput.setText(times[1].format(timeFormatter))
                }
            }
            "Weekly" -> {
                times?.firstOrNull()?.let { time ->
                    binding.weeklyTimeInput.setText(time.format(timeFormatter))
                }

                val selectedDays = viewModel.getSelectedDays()
                if (!selectedDays.isNullOrEmpty()) {
                    selectedDays.forEach { day ->
                        val chipId = when (day) {
                            DayOfWeek.MONDAY -> R.id.mondayChip
                            DayOfWeek.TUESDAY -> R.id.tuesdayChip
                            DayOfWeek.WEDNESDAY -> R.id.wednesdayChip
                            DayOfWeek.THURSDAY -> R.id.thursdayChip
                            DayOfWeek.FRIDAY -> R.id.fridayChip
                            DayOfWeek.SATURDAY -> R.id.saturdayChip
                            DayOfWeek.SUNDAY -> R.id.sundayChip
                        }
                        binding.daysChipGroup.findViewById<Chip>(chipId)?.isChecked = true
                    }
                }
            }
            "Cyclic" -> {
                times?.firstOrNull()?.let { time ->
                    binding.cyclicTimeInput.setText(time.format(timeFormatter))
                }
                viewModel.intakeDays.value?.let { days ->
                    binding.intakeDaysInput.setText(days.toString())
                    Log.d(tag, "Setting intake days: $days")
                }
                viewModel.pauseDays.value?.let { days ->
                    binding.pauseDaysInput.setText(days.toString())
                    Log.d(tag, "Setting pause days: $days")
                }
            }
            "On Demand" -> {
                viewModel.maxDoses.value?.let { doses ->
                    binding.maxDosesInput.setText(doses.toString())
                }
                viewModel.minHoursBetween.value?.let { hours ->
                    binding.minHoursBetweenInput.setText(hours.toString())
                }
            }
        }
    }


    private fun parseTimes(input: String): List<LocalTime> {
        if (input.isBlank()) return emptyList()

        return if (input.contains(",")) {
            // Parse comma-separated times
            input.split(",")
                .map { it.trim() }
                .mapNotNull {
                    try {
                        LocalTime.parse(it, timeFormatter)
                    } catch (e: Exception) {
                        null
                    }
                }
        } else {
            try {
                listOf(LocalTime.parse(input.trim(), timeFormatter))
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun saveMedication(userId: String) {
        processMedication(userId, false)
    }

    private fun updateMedication(userId: String) {
        processMedication(userId, true)
    }



    private fun processMedication(userId: String, isUpdate: Boolean) {
        val currentTime = LocalTime.now()
        val medicationName = viewModel.getName()

        // Schedule notifications based on frequency type
        when (viewModel.getFrequencyType()) {
            "Twice Daily" -> {
                val selectedLocalTimes = viewModel.getSelectedTimes() ?: listOf(currentTime, currentTime)
                var scheduledDateTimes = selectedLocalTimes.map { time ->
                    LocalDateTime.of(LocalDate.now(), time)
                }
                if (scheduledDateTimes[0].isBefore(LocalDateTime.now())) {
                    scheduledDateTimes = scheduledDateTimes.map { it.plusDays(1) }
                }
                val triggerTimeInMillis = scheduledDateTimes.map { scheduledDateTime ->
                    scheduledDateTime
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
                scheduleNotifications(medicationName!!, triggerTimeInMillis)
            }
            "Weekly" -> {
                val selectedDays = viewModel.getSelectedDays() ?: emptySet()
                val selectedTime = viewModel.getSelectedTimes()?.firstOrNull() ?: currentTime

                if (selectedDays.isNotEmpty()) {
                    val today = LocalDate.now().dayOfWeek
                    val scheduledDateTimes = selectedDays.map { day ->
                        // Calculate days to add to get to the next occurrence of this day
                        val daysToAdd = (day.value - today.value + 7) % 7
                        val date = if (daysToAdd == 0 && selectedTime.isBefore(currentTime)) {
                            LocalDate.now().plusDays(7) // Schedule for next week
                        } else if (daysToAdd == 0) {
                            LocalDate.now() // Today, but time is still in future
                        } else {
                            LocalDate.now().plusDays(daysToAdd.toLong())
                        }

                        LocalDateTime.of(date, selectedTime)
                    }

                    val triggerTimeInMillis = scheduledDateTimes.map { dateTime ->
                        dateTime
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    }

                    Log.d(tag, "Weekly schedule - selected days: $selectedDays, times: $triggerTimeInMillis")
                    scheduleNotifications(medicationName!!, triggerTimeInMillis)
                }
            }
            "Once Daily", "Cyclic" -> {
                val selectedLocalTime = viewModel.getSelectedTimes()?.firstOrNull() ?: currentTime
                var scheduledDateTime = LocalDateTime.of(LocalDate.now(), selectedLocalTime)
                if (scheduledDateTime.isBefore(LocalDateTime.now())) {
                    scheduledDateTime = scheduledDateTime.plusDays(1)
                }

                val triggerTimeInMillis = scheduledDateTime
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                scheduleNotifications(medicationName!!, listOf(triggerTimeInMillis))
            }
        }

        if (isUpdate) {
            viewModel.updateMedication(userId)
        } else {
            viewModel.saveMedication(userId)
        }

        navigateToNextFragment()

        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            val action = if (isUpdate) "updated" else "saved"
            if (success) {
                Log.d(tag, "Successfully $action medication")
                displayMessage("Successfully $action medication")
            } else {
                Log.d(tag, "Failed to $action medication")
                displayMessage("Failed to $action medication")
            }
        }
    }

    private fun scheduleNotifications(medicationName: String, timeInMillisList: List<Long>) {
        val notificationHelper = (requireActivity().application as MedicationApp).notificationHelper

        timeInMillisList.forEach { time ->
            val instant = Instant.ofEpochMilli(time)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
            val formattedDate = formatter.format(instant)
            Log.d(tag, "Scheduling notification at: $formattedDate for $medicationName")

            notificationHelper.scheduleNotification(
                medicationName = medicationName,
                timeInMillis = time
            )
        }
    }


    private fun navigateToNextFragment() {
        if (isAdded && !isStateSaved) {
            if (viewModel.getIsEditing() == true) {
                // Clear all fragments up to home, then navigate to prescriptions
                findNavController().navigate(
                    R.id.action_addMedicationScheduleFragment_to_prescriptionsFragment,
                    null,
                    navOptions {
                        popUpTo(R.id.homeFragment) {
                            inclusive = false  // Keep home fragment as base
                        }
                    }
                )
            } else {
                // Navigate to home and clear everything else
                findNavController().navigate(
                    R.id.action_addMedicationScheduleFragment_to_homeFragment,
                    null,
                    navOptions {
                        popUpTo(R.id.homeFragment) {
                            inclusive = true  // Clear everything including home
                        }
                    }
                )
            }
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

    override fun onResume() {
        super.onResume()
        setupViews()

    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(tag,"In on destroy...")
        _binding = null
    }
}