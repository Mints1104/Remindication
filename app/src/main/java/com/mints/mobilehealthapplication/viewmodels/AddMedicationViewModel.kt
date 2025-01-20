package com.mints.mobilehealthapplication.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mints.mobilehealthapplication.data.FireStoreRepository
import com.mints.mobilehealthapplication.data.Medication
import kotlinx.coroutines.launch

class AddMedicationViewModel : ViewModel() {

    private var tag = "AddMedicationViewModel"

    val saveResult = MutableLiveData<Boolean>()
    private val _medicationName = MutableLiveData<String>()
    private val _dosage = MutableLiveData<String>()
    private val _notes = MutableLiveData<String>()
    private val _validationState = MutableLiveData<ValidationState>()
    private val _frequency = MutableLiveData<String>()
    val frequency: LiveData<String> = _frequency
    private val _currentStage = MutableLiveData<FormStage>(FormStage.BASIC_INFO)
    val currentStage: LiveData<FormStage> = _currentStage

    val validationState: LiveData<ValidationState> = _validationState
    val medicationName: LiveData<String> = _medicationName
    val dosage: LiveData<String> = _dosage
    val notes: LiveData<String> = _notes

    sealed class ValidationState {
        data object Valid : ValidationState()
        data class Invalid(val message: String) : ValidationState()
        data object Initial : ValidationState()
    }

    sealed class FormStage {
        object BASIC_INFO : FormStage()
        object FREQUENCY : FormStage()
    }

    fun resetAllData() {
        _medicationName.value = ""
        _dosage.value = ""
        _notes.value = ""
        _frequency.value = ""
        _validationState.value = ValidationState.Initial
        _currentStage.value = FormStage.BASIC_INFO
    }


    fun resetValidationState() {
        _validationState.value = ValidationState.Initial
    }

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
                _currentStage.value = FormStage.FREQUENCY // Move to next stage
                true
            }
        }
    }

    fun validateFrequency(): Boolean {
        return if (_frequency.value.isNullOrBlank()) {
            _validationState.value = ValidationState.Invalid("Frequency is required")
            false
        } else {
            _validationState.value = ValidationState.Valid
            true
        }
    }


    fun saveMedication(uid: String, medication: Medication) {
        Log.d("MedicationViewModel", "Saving medication for user: $uid")
        Log.d("MedicationViewModel", "Medication details: $medication")

        viewModelScope.launch {
            val success = FireStoreRepository.saveMedication(uid, medication)
            Log.d("MedicationViewModel", "Medication save result: $success")
            saveResult.postValue(success)
        }
    }
}