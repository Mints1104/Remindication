package com.mints.mobilehealthapplication.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mints.mobilehealthapplication.data.MedicationResult
import com.mints.mobilehealthapplication.data.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class MedicationInfoViewModel : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val medicationResult: MedicationResult) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun fetchMedicationInfo(drugName: String) {
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                Log.d("MedicationInfoViewModel", "Starting fetch for: $drugName")
                val searchQuery = "openfda.brand_name:$drugName"

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.fdaApi.getMedicationInfo(searchQuery)
                }

                if (response.results.isNotEmpty()) {
                    val result = response.results[0]
                    _uiState.value = UiState.Success(result)
                } else {
                    _uiState.value = UiState.Error("No information found for $drugName")
                }
            } catch (e: HttpException) {
                Log.e("MedicationInfoViewModel", "HTTP error: ${e.code()}", e)
                val errorMessage = if (e.code() == 404) {
                    "No information found for $drugName"
                } else {
                    "Unable to retrieve information. Please try again later."
                }
                _uiState.value = UiState.Error(errorMessage)
            } catch (e: Exception) {
                Log.e("MedicationInfoViewModel", "Error fetching medication info", e)
                _uiState.value = UiState.Error("Error fetching data: ${e.message}")
            }
        }
    }

    fun formatMedicationInfo(text: String): String {
        val paragraphs = text.split("\n\n", "\r\n\r\n")
        val formattedText = StringBuilder()

        for (paragraph in paragraphs) {
            if (paragraph.trim().isEmpty()) continue

            val cleanParagraph = paragraph.replace("\n", " ").trim()

            if (cleanParagraph.contains(". ") && cleanParagraph.length > 100) {
                val sentences = cleanParagraph.split(". ")
                for (sentence in sentences) {
                    if (sentence.trim().isEmpty()) continue

                    val bulletPoint = sentence.trim() + if (sentence.endsWith(".")) "" else "."
                    formattedText.append("• $bulletPoint\n")
                }
                formattedText.append("\n")
            } else {
                formattedText.append(cleanParagraph)
                formattedText.append("\n\n")
            }
        }

        return formattedText.toString().trim()
    }
}