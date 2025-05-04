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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
    import java.time.ZoneId
    import java.time.temporal.TemporalAdjusters
    import java.util.Date

    class AddMedicationViewModel : ViewModel(
    ) {

        private val _medicationName = MutableLiveData("").apply {
            observeForever { newValue ->
                Log.d("AddMedicationViewModel", "medicationName value changed to: $newValue")
            }
        }

        val medicationName: LiveData<String> = _medicationName
        private val _medicationId = MutableLiveData("")
        val medicationId: LiveData<String> = _medicationId

        private val _dosage = MutableLiveData("")
        val dosage: LiveData<String> = _dosage



        private val _notes = MutableLiveData("")
        val notes: LiveData<String> = _notes

        private val _frequency = MutableLiveData("")
        val frequency: LiveData<String> = _frequency

        private val _frequencyType = MutableLiveData("")
        val frequencyType: LiveData<String> = _frequencyType

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



        private val _isEditing = MutableLiveData(false)
        val isEditing: LiveData<Boolean> = _isEditing



        private val _validationState = MutableLiveData<ValidationState>(ValidationState.Initial)
        val validationState: LiveData<ValidationState> = _validationState

        private val _currentStage = MutableLiveData<FormStage>(FormStage.BASIC_INFO)
        val currentStage: LiveData<FormStage> = _currentStage

        val saveResult = MutableLiveData<Boolean>()

        sealed class ValidationState {
            data object Valid : ValidationState()
            data class Invalid(val message: String) : ValidationState()
            data object Initial : ValidationState()
        }

        sealed class FormStage {
            data object BASIC_INFO : FormStage()
            data object FREQUENCY : FormStage()
            data object SCHEDULE : FormStage()
        }






        fun setSelectedDays(days: Set<DayOfWeek>) {
            _selectedDays.value = days
        }

        fun getSelectedDays(): Set<DayOfWeek>? = _selectedDays.value

        fun setSelectedTimes(times: List<LocalTime>) {
            _selectedTimes.value = times
        }

        fun getSelectedTimes(): List<LocalTime>? = _selectedTimes.value


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




        fun setIsEditing(status:Boolean) {
            _isEditing.value = status
        }

        fun getIsEditing(): Boolean? = _isEditing.value


        fun validateFrequency(): Boolean {
            return if (_frequency.value.isNullOrBlank()) {
                _validationState.value = ValidationState.Invalid("Frequency is required")
                false
            } else {
                _validationState.value = ValidationState.Valid
                _currentStage.value = FormStage.SCHEDULE
                true
            }
        }

        fun validateSchedule(): Boolean {
            Log.d("AddMedicationViewModel", "frequency.value: ${_frequency.value}")
            Log.d("AddMedicationViewModel", "frequencyType.value: ${_frequencyType.value}")

            val isValid = when (_frequency.value) {
                "Once Daily", "Twice Daily" -> validateDailySchedule()
                "Weekly" -> validateWeeklySchedule()
                "Cyclic" -> validateCyclicSchedule()
                "On Demand" -> validateOnDemand()
                else -> {
                    Log.d("AddMedicationViewModel", "No matching frequency, validation fails")
                    false
                }
            }

            if (isValid) {
                _validationState.value = ValidationState.Valid
            } else {
                Log.d("AddMedicationViewModel", "Schedule validation failed")
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
                    Log.d("AddMedicationViewModel", "Selected days are empty")
                    false
                }
                _selectedTimes.value.isNullOrEmpty() -> {
                    setValidationError("Please select at least one time")
                    Log.d("AddMedicationViewModel", "Selected times are empty")
                    false
                }
                else -> true
            }
        }

        private fun validateCyclicSchedule(): Boolean {
            return when {
                _intakeDays.value == null -> {
                    setValidationError("Please enter intake days")
                    Log.d("AddMedicationViewModel", "Intake days are null")
                    false
                }
                _pauseDays.value == null -> {
                    setValidationError("Please enter pause days")
                    Log.d("AddMedicationViewModel", "Pause days are null")
                    false
                }
                _selectedTimes.value.isNullOrEmpty() -> {
                    setValidationError("Please select at least one time")
                    Log.d("AddMedicationViewModel", "Selected times are empty")
                    false
                }
                !ScheduleValidator.isValidCyclicSchedule(
                    _intakeDays.value!!,
                    _pauseDays.value!!,
                    _selectedTimes.value!!
                ) -> {
                    setValidationError("Invalid cyclic schedule parameters")
                    Log.d("AddMedicationViewModel", "Invalid cyclic schedule parameters")
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
            _validationState.value = ValidationState.Initial
            _currentStage.value = FormStage.BASIC_INFO
            _isEditing.value = false
        }

        fun resetValidationState() {
            _validationState.value = ValidationState.Initial
        }

        suspend fun getMedicationDetails(uid: String, medicationId: String) {
                val med = FireStoreRepository.getMedicationDetails(uid, medicationId)
                updateMedicationName(med.name)
                updateDosage(med.dosage)
                updateNotes(med.notes)
                updateFrequency(med.schedule.formattedFrequency)
                updateFrequencyType(med.schedule.frequencyType)
                updateMedicationId(med.id!!)

            when (val schedule = med.schedule) {
                is MedicationSchedule.Daily -> {
                    _selectedTimes.value = schedule.times
                }
                is MedicationSchedule.WeeklySchedule -> {
                    _selectedTimes.value = schedule.times
                    _selectedDays.value = schedule.days.toSet()
                }
                is MedicationSchedule.Cyclic -> {
                    _selectedTimes.value = schedule.times
                    _intakeDays.value = schedule.intakeDays
                    _pauseDays.value = schedule.pauseDays
                }
                is MedicationSchedule.OnDemand -> {
                    _maxDoses.value = schedule.maxDailyDoses
                    _minHoursBetween.value = schedule.minTimeBetweenDoses
                }
                else -> {}
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

        fun updateMedication(userId: String) {
            viewModelScope.launch {
                try {
                    Log.d("AddMedicationVM", "Attempting to update medication for user: $userId")
                    val medication = createMedication()
                    medication.id = getMedicationId()
                    Log.d("AddMedicationVM", "Medication created: ${medication.id}")

                    val success = FireStoreRepository.updateMedication(userId, medication)
                    Log.d("AddMedicationVM", "Update result: $success")

                    saveResult.postValue(success)
                    if (success) resetAllData()
                } catch (e: Exception) {
                    Log.e("AddMedicationVM", "Update failed for user: $userId", e)
                    saveResult.postValue(false)
                }
            }
        }






        fun testDateLogic() {
            val testTime = LocalTime.of(20, 0)
            val now = LocalDateTime.now()

            val isTomorrow = testTime.atDate(now.toLocalDate()).isBefore(now)
            val resultDate = if (isTomorrow) "Tomorrow" else "Today"

            Log.d("TEST", "8:00 PM should be $resultDate")
        }

        private fun calculateWeeklyDueDates(days: List<DayOfWeek>, times: List<LocalTime>): List<LocalDateTime> {
                return days.flatMap { day ->
                    times.map { time ->
                        LocalDate.now()
                            .with(TemporalAdjusters.nextOrSame(day))
                            .atTime(time)
                            .let { if (it.isBefore(LocalDateTime.now())) it.plusWeeks(1) else it }
                    }
                }
            }





            private fun calculateDueDates(times: List<LocalTime>): List<LocalDateTime> {
                return times.map { time ->
                    val now = LocalDateTime.now()
                    val scheduledDateTime = time.atDate(now.toLocalDate())
                    if (scheduledDateTime.isBefore(now)) scheduledDateTime.plusDays(1)
                    else scheduledDateTime
                }
            }


        private fun createSchedule(): MedicationSchedule {
            return when (_frequency.value) {
                "Once Daily", "Twice Daily" ->
                    MedicationSchedule.Daily(
                        frequency = DailyFrequency.fromInt(_selectedTimes.value?.size ?: 1),
                        times = _selectedTimes.value ?: emptyList(),
                        nextDueDates = calculateDueDates(_selectedTimes.value ?: emptyList()),
                    )
                "Weekly" -> MedicationSchedule.WeeklySchedule(
                    days = _selectedDays.value?.toList() ?: emptyList(),
                    times = _selectedTimes.value ?: emptyList(),
                    nextDueDates = calculateWeeklyDueDates(
                        _selectedDays.value?.toList() ?: emptyList(),
                        _selectedTimes.value ?: emptyList()
                    ),
                )
                "Cyclic" -> {
                    val intakeDays = _intakeDays.value
                        ?: throw IllegalStateException("Missing intake days")
                    val pauseDays = _pauseDays.value
                        ?: throw IllegalStateException("Missing pause days")
                    val cyclicTimes = _selectedTimes.value ?: emptyList()
                    val currentCycleStart = Timestamp(Date())
                    val nextDueDates = calculateCyclicDueDates(intakeDays, cyclicTimes, currentCycleStart)
                    MedicationSchedule.Cyclic(
                        intakeDays = intakeDays,
                        pauseDays = pauseDays,
                        times = cyclicTimes,
                        nextDueDates = nextDueDates,
                        currentCycleStartDate = currentCycleStart
                    )
                }
                "On Demand" -> MedicationSchedule.OnDemand(
                    maxDailyDoses = _maxDoses.value,
                    minTimeBetweenDoses = _minHoursBetween.value,
                    instructions = _notes.value ?: ""
                )
                else -> throw IllegalArgumentException("Invalid schedule type")
            }
        }


        private fun calculateCyclicDueDates(
            intakeDays: Int,
            times: List<LocalTime>,
            currentCycleStartDate: Timestamp?
        ): List<LocalDateTime> {
            val startDate: LocalDate = currentCycleStartDate?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())
                ?.toLocalDate() ?: LocalDate.now()

            val dueDates = mutableListOf<LocalDateTime>()
            for (day in 0 until intakeDays) {
                val date = startDate.plusDays(day.toLong())
                times.forEach { time ->
                    dueDates.add(LocalDateTime.of(date, time))
                }
            }
            return dueDates.sorted()
        }

        private fun setValidationError(message: String) {
            _validationState.value = ValidationState.Invalid(message)
        }

        fun updateFrequencyType(frequencyType: String) {
            _frequencyType.value = frequencyType
            _frequency.value = frequencyType
        }

        fun updateFrequency(frequency: String) {
            _frequency.value = frequency
            _frequencyType.value = frequency
        }

        fun getFrequencyType(): String? = _frequencyType.value
        fun getMedicationId(): String? = _medicationId.value




        fun updateMedicationName(name: String) {
            _medicationName.value = name
        }

        fun updateMedicationId(medicationId:String) {
            _medicationId.value = medicationId
        }



        fun updateDosage(dosage: String) {
            _dosage.value = dosage
        }

        fun updateNotes(notes: String) {
            _notes.value = notes
        }

        fun getFrequency(): String? = _frequency.value

        fun getName(): String? =  _medicationName.value

        fun getNotes(): String? = _notes.value
        fun getDosage(): String? = _dosage.value

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
                    _validationState.value = ValidationState.Valid
                    _currentStage.value = FormStage.FREQUENCY
                    true
                }
            }
        }
    }