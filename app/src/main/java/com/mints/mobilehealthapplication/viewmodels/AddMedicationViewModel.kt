package com.mints.mobilehealthapplication.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.mints.mobilehealthapplication.data.DailyFrequency
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.ScheduleValidator
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class AddMedicationViewModel : ViewModel() {

    // LiveData Properties
    private val _medicationName = MutableLiveData<String>("")
    val medicationName: LiveData<String> = _medicationName

    private val _dosage = MutableLiveData<String>("")
    val dosage: LiveData<String> = _dosage

    private val _notes = MutableLiveData<String>("")
    val notes: LiveData<String> = _notes

    private val _frequency = MutableLiveData<String>("")
    val frequency: LiveData<String> = _frequency

    private val _selectedDays = MutableLiveData<Set<DayOfWeek>>(emptySet())
    val selectedDays: LiveData<Set<DayOfWeek>> = _selectedDays

    private val _selectedTimes = MutableLiveData<List<LocalTime>>(emptyList())
    val selectedTimes: LiveData<List<LocalTime>> = _selectedTimes

    private val _intakeDays = MutableLiveData<Int?>(null)
    val intakeDays: LiveData<Int?> = _intakeDays

    private val _pauseDays = MutableLiveData<Int?>(null)
    val pauseDays: LiveData<Int?> = _pauseDays

    private val _maxDoses = MutableLiveData<Int?>(null)
    val maxDoses: LiveData<Int?> = _maxDoses

    private val _minHoursBetween = MutableLiveData<Int?>(null)
    val minHoursBetween: LiveData<Int?> = _minHoursBetween

    private val _withFood = MutableLiveData<Boolean>(false)
    val withFood: LiveData<Boolean> = _withFood

    private val _validationState = MutableLiveData<ValidationState>(ValidationState.Initial)
    val validationState: LiveData<ValidationState> = _validationState

    private val _currentStage = MutableLiveData<FormStage>(FormStage.BASIC_INFO)
    val currentStage: LiveData<FormStage> = _currentStage

    val saveResult = MutableLiveData<Boolean>()

    sealed class ValidationState {
        object Valid : ValidationState()
        data class Invalid(val message: String) : ValidationState()
        object Initial : ValidationState()
    }

    sealed class FormStage {
        object BASIC_INFO : FormStage()
        object FREQUENCY : FormStage()
        object SCHEDULE : FormStage()
    }

    // Data methods
    fun setSelectedDays(days: Set<DayOfWeek>) {
        _selectedDays.value = days
    }

    fun setSelectedTimes(times: List<LocalTime>) {
        _selectedTimes.value = times
    }

    fun updateIntakeDays(days: Int) {
        _intakeDays.value = days
    }

    fun updatePauseDays(days: Int) {
        _pauseDays.value = days
    }

    fun updateMaxDoses(doses: Int?) {
        _maxDoses.value = doses
    }

    fun updateMinHoursBetween(hours: Int?) {
        _minHoursBetween.value = hours
    }

    fun setWithFoodStatus(checked: Boolean) {
        _withFood.value = checked
    }

    // Validation methods
    fun validateBasicInfo(): Boolean {
        return when {
            _medicationName.value.isNullOrBlank() -> {
                _validationState.value = ValidationState.Invalid("Medication name is required")
                false
            }
            _dosage.value.isNullOrBlank() -> {
                _validationState.value = ValidationState.Invalid("Dosage is required")
                false
            }
            else -> {
                // Update both validation state AND current stage
                _validationState.value = ValidationState.Valid
                _currentStage.value = FormStage.FREQUENCY
                true
            }
        }
    }

    fun validateFrequency(): Boolean {
        return if (_frequency.value.isNullOrBlank()) {
            _validationState.value = ValidationState.Invalid("Frequency is required")
            false
        } else {
            // Update both validation state AND current stage
            _validationState.value = ValidationState.Valid
            _currentStage.value = FormStage.SCHEDULE
            true
        }
    }

    fun validateSchedule(): Boolean {
        val isValid = when (_frequency.value) {
            "Once Daily", "Twice Daily" -> validateDailySchedule()
            "Weekly" -> validateWeeklySchedule()
            "Cyclic" -> validateCyclicSchedule()
            "On Demand" -> validateOnDemand()
            else -> false
        }

        if (isValid) {
            _validationState.value = ValidationState.Valid
        }
        return isValid
    }

    private fun validateDailySchedule(): Boolean {
        return if (_selectedTimes.value.isNullOrEmpty()) {
            setValidationError("Please select at least one time")
            false
        } else true
    }

    private fun validateWeeklySchedule(): Boolean {
        return when {
            _selectedDays.value.isNullOrEmpty() -> {
                setValidationError("Please select at least one day")
                false
            }
            _selectedTimes.value.isNullOrEmpty() -> {
                setValidationError("Please select at least one time")
                false
            }
            else -> true
        }
    }

    private fun validateCyclicSchedule(): Boolean {
        return when {
            _intakeDays.value == null -> {
                setValidationError("Please enter intake days")
                false
            }
            _pauseDays.value == null -> {
                setValidationError("Please enter pause days")
                false
            }
            _selectedTimes.value.isNullOrEmpty() -> {
                setValidationError("Please select at least one time")
                false
            }
            !ScheduleValidator.isValidCyclicSchedule(
                _intakeDays.value!!,
                _pauseDays.value!!,
                _selectedTimes.value!!
            ) -> {
                setValidationError("Invalid cyclic schedule parameters")
                false
            }
            else -> true
        }
    }

    private fun validateOnDemand(): Boolean {
        return if (!ScheduleValidator.isValidOnDemandSchedule(
                _maxDoses.value,
                _minHoursBetween.value
            )) {
            setValidationError("Invalid on-demand parameters")
            false
        } else true
    }

    // State management
    fun resetAllData() {
        _medicationName.value = ""
        _dosage.value = ""
        _notes.value = ""
        _frequency.value = ""
        _selectedDays.value = emptySet()
        _selectedTimes.value = emptyList()
        _intakeDays.value = null
        _pauseDays.value = null
        _maxDoses.value = null
        _minHoursBetween.value = null
        _withFood.value = false
        _validationState.value = ValidationState.Initial
        _currentStage.value = FormStage.BASIC_INFO
    }

    fun resetValidationState() {
        _validationState.value = ValidationState.Initial
    }

    // Save functionality
    fun saveMedication(userId: String) {
        viewModelScope.launch {
            try {
                val medication = createMedication()
                val success = FireStoreRepository.saveMedication(userId, medication)
                saveResult.postValue(success)
                if (success) resetAllData()
            } catch (e: Exception) {
                Log.e("AddMedicationVM", "Save failed", e)
                saveResult.postValue(false)
            }
        }
    }

    private fun createMedication(): Medication {
        return Medication(
            name = _medicationName.value ?: "",
            dosage = _dosage.value ?: "",
            schedule = createSchedule(),
            notes = _notes.value ?: "",
            createdAt = Timestamp.now(),
            active = true,
            lastModified = Timestamp.now()
        )
    }

    private fun createSchedule(): MedicationSchedule {
        return when (_frequency.value) {
            "Once Daily", "Twice Daily" -> MedicationSchedule.Daily(
                frequency = DailyFrequency.fromInt(_selectedTimes.value?.size ?: 1),
                times = _selectedTimes.value ?: emptyList(),
                withFood = _withFood.value ?: false
            )
            "Weekly" -> MedicationSchedule.WeeklySchedule(
                days = _selectedDays.value?.toList() ?: emptyList(),
                times = _selectedTimes.value ?: emptyList(),
                withFood = _withFood.value ?: false
            )
            "Cyclic" -> MedicationSchedule.Cyclic(
                intakeDays = _intakeDays.value ?: throw IllegalStateException("Missing intake days"),
                pauseDays = _pauseDays.value ?: throw IllegalStateException("Missing pause days"),
                times = _selectedTimes.value ?: emptyList()
            )
            "On Demand" -> MedicationSchedule.OnDemand(
                maxDailyDoses = _maxDoses.value,
                minTimeBetweenDoses = _minHoursBetween.value,
                instructions = _notes.value ?: ""
            )
            else -> throw IllegalArgumentException("Invalid schedule type")
        }
    }

    private fun setValidationError(message: String) {
        _validationState.value = ValidationState.Invalid(message)
    }

    // Update methods
    fun updateFrequency(frequency: String) {
        _frequency.value = frequency
    }

    fun updateMedicationName(name: String) {
        _medicationName.value = name
    }

    fun updateDosage(dosage: String) {
        _dosage.value = dosage
    }

    fun updateNotes(notes: String) {
        _notes.value = notes
    }

    fun getFrequency(): String? = _frequency.value
}