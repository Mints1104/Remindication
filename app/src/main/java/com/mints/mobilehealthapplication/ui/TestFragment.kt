package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mints.mobilehealthapplication.databinding.FragmentTestBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TestFragment : Fragment() {
    private var _binding: FragmentTestBinding? = null
    private val binding get() = _binding!!

    // We'll receive the medication name via arguments.
    private var medicationName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the medication name from arguments
        medicationName = arguments?.getString("MEDICATION_NAME")
        if (medicationName.isNullOrEmpty()) {
            binding.medicationInfo.text = "No medication name provided."
        } else {
            // Update header with the medication name
            binding.medicationNameHeader.text = medicationName
            // Automatically fetch data for the given medication name
            fetchMedicationInfo(medicationName!!)
        }
    }

    private fun fetchMedicationInfo(drugName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("TestFragment", "Starting fetch for: $drugName")
                val apiUrl = "https://api.fda.gov/drug/label.json?search=openfda.brand_name:$drugName&limit=1"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d("TestFragment", "Response code: $responseCode")
                if (responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("TestFragment", "Fetched data: $responseText")
                    val json = JSONObject(responseText)
                    if (json.has("results") && json.getJSONArray("results").length() > 0) {
                        val results = json.getJSONArray("results").getJSONObject(0)
                        val medicationInfo = buildMedicationInfoString(results)
                        withContext(Dispatchers.Main) {
                            binding.medicationInfo.text = medicationInfo
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            binding.medicationInfo.text = "No information found for $drugName"
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.medicationInfo.text =  "No information found for $drugName"
                        Log.d("TestFragment", "Error: API returned status code $responseCode")
                    }
                }
            } catch (e: Exception) {
                Log.e("TestFragment", "Error fetching medication info", e)
                withContext(Dispatchers.Main) {
                    binding.medicationInfo.text = "Error fetching data: ${e.message}"
                }
            }
        }
    }


    private fun buildMedicationInfoString(results: JSONObject): String {
        val stringBuilder = StringBuilder()

        // Brand & Generic Name
        val brandName = results.optJSONObject("openfda")?.optJSONArray("brand_name")?.optString(0) ?: "Unknown"
        val genericName = results.optJSONObject("openfda")?.optJSONArray("generic_name")?.optString(0) ?: "Unknown"
        stringBuilder.append("Brand Name: $brandName\n")
        stringBuilder.append("Generic Name: $genericName\n\n")

        // Active Ingredient
        appendSection(stringBuilder, results, "active_ingredient", "Active Ingredient")

        // Purpose
        appendSection(stringBuilder, results, "purpose", "Purpose")

        // Indications & Usage
        appendSection(stringBuilder, results, "indications_and_usage", "Uses")

        // Warnings
        appendSection(stringBuilder, results, "warnings", "Warnings")

        // Dosage & Administration
        appendSection(stringBuilder, results, "dosage_and_administration", "Directions")

        // Other Key Information
        appendSection(stringBuilder, results, "do_not_use", "Do Not Use")
        appendSection(stringBuilder, results, "ask_doctor", "Ask Doctor Before Use")
        appendSection(stringBuilder, results, "stop_use", "Stop Use")
        appendSection(stringBuilder, results, "pregnancy_or_breast_feeding", "Pregnancy or Breast-Feeding")
        appendSection(stringBuilder, results, "storage_and_handling", "Storage Information")
        appendSection(stringBuilder, results, "inactive_ingredient", "Inactive Ingredients")

        return stringBuilder.toString()
    }

    private fun appendSection(stringBuilder: StringBuilder, json: JSONObject, key: String, sectionTitle: String) {
        if (json.has(key) && json.getJSONArray(key).length() > 0) {
            val content = json.getJSONArray(key).getString(0)
            stringBuilder.append("$sectionTitle:\n")
            stringBuilder.append(formatMedicationInfo(content))
            stringBuilder.append("\n\n")
        }
    }

    private fun formatMedicationInfo(text: String): String {
        // Convert blocks of text to bullet points for better readability
        val lines = text.split(". ")
        val formattedText = StringBuilder()
        for (line in lines) {
            if (line.trim().isNotEmpty()) {
                val formattedLine = line.trim().replace("\n", " ")
                val lineWithPeriod = if (formattedLine.endsWith(".")) formattedLine else "$formattedLine."
                formattedText.append("• $lineWithPeriod\n")
            }
        }
        return formattedText.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
