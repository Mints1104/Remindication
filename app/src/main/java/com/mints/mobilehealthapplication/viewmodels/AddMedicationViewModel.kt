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

    val validationState: LiveData<ValidationState> = _validationState
    val medicationName: LiveData<String> = _medicationName
    val dosage: LiveData<String> = _dosage
    val notes: LiveData<String> = _notes

    sealed class ValidationState {
        data object Valid : ValidationState()
        data class Invalid(val message: String) : ValidationState()
        data object Initial : ValidationState()
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


    fun validateInputs(): Boolean {
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
                Log.d(tag,"Input validated successfully")
                _validationState.value = ValidationState.Valid
                true
            }
        }
    }

    fun getStage1Data(): Stage1Data {
        return Stage1Data(
            name = _medicationName.value ?: "",
            dosage = _dosage.value ?: "",
            notes = _notes.value ?: ""
        )
    }

    data class Stage1Data(
        val name: String,
        val dosage: String,
        val notes: String
    )

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