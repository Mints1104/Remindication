package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.mints.mobilehealthapplication.R
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

    private var medicationName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        medicationName = arguments?.getString("MEDICATION_NAME")
        if (medicationName.isNullOrEmpty()) {
            showError("No medication name provided")
        } else {
            (requireActivity() as MainActivity).updateToolBarTitle("Medication Info for ${medicationName!!}")

            fetchMedicationInfo(medicationName!!)
        }
    }

    private fun fetchMedicationInfo(drugName: String) {
        // Show loading state
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.infoContainer.visibility = View.GONE
        binding.errorMessage.visibility = View.GONE
        binding.medicationHeaderCard.visibility = View.GONE

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
                    val json = JSONObject(responseText)

                    if (json.has("results") && json.getJSONArray("results").length() > 0) {
                        val results = json.getJSONArray("results").getJSONObject(0)

                        withContext(Dispatchers.Main) {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.infoContainer.visibility = View.VISIBLE
                            binding.medicationHeaderCard.visibility = View.VISIBLE

                            displayMedicationInfo(results)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showError("No information found for $drugName")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val errorMessage = if (responseCode == 404) {
                            "No information found for $drugName"
                        } else {
                            "Unable to retrieve information. Please try again later."
                        }
                        showError(errorMessage)

                    }
                }
            } catch (e: Exception) {
                Log.e("TestFragment", "Error fetching medication info", e)
                withContext(Dispatchers.Main) {
                    showError("Error fetching data: ${e.message}")
                }
            }
        }
    }

    private fun showError(message: String) {
        binding.loadingIndicator.visibility = View.GONE
        binding.infoContainer.visibility = View.GONE
        binding.medicationHeaderCard.visibility = View.GONE
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = message
    }

    private fun displayMedicationInfo(results: JSONObject) {
        // Clear any existing content
        binding.infoContainer.removeAllViews()

        // Display medication name and generic name in the header
        val openfda = results.optJSONObject("openfda")
        val brandName = openfda?.optJSONArray("brand_name")?.optString(0) ?: "Unknown"
        val genericName = openfda?.optJSONArray("generic_name")?.optString(0) ?: "Unknown"

        binding.medicationNameHeader.text = brandName
        binding.medicationGenericName.text = genericName

        // Key medication information sections (expanded by default)
        addInfoSection(R.drawable.ic_directions, "How to Use",
            results, "dosage_and_administration", true)

        addInfoSection(R.drawable.ic_warning, "Warnings & Precautions",
            results, "warnings", true)

        addInfoSection(R.drawable.ic_uses, "Uses",
            results, "indications_and_usage", true)

        // Secondary information (collapsed by default)
        addInfoSection(R.drawable.ic_ingredient, "Active Ingredients",
            results, "active_ingredient", false)

        addInfoSection(R.drawable.ic_stop, "When to Stop Use",
            results, "stop_use", false)

        addInfoSection(R.drawable.ic_exclamation, "Do Not Use If",
            results, "do_not_use", false)

        addInfoSection(R.drawable.ic_ask_doctor, "Ask Doctor",
            results, "ask_doctor", false)

        addInfoSection(R.drawable.ic_pregnancy, "Pregnancy or Breastfeeding",
            results, "pregnancy_or_breast_feeding", false)

        addInfoSection(R.drawable.ic_storage, "Storage Information",
            results, "storage_and_handling", false)

        addInfoSection(R.drawable.ic_ingredient, "Inactive Ingredients",
            results, "inactive_ingredient", false)
    }

    private fun addInfoSection(iconResId: Int, title: String, results: JSONObject,
                               jsonKey: String, expandedByDefault: Boolean) {
        // Skip if this section doesn't exist
        if (!results.has(jsonKey) || results.getJSONArray(jsonKey).length() == 0) {
            return
        }

        val content = results.getJSONArray(jsonKey).getString(0)
        if (content.isBlank()) return

        // Inflate the section card
        val inflater = LayoutInflater.from(requireContext())
        val sectionCard = inflater.inflate(
            R.layout.medication_info_card,
            binding.infoContainer,
            false
        ) as MaterialCardView


        // Set up the card content
        val sectionIcon = sectionCard.findViewById<ImageView>(R.id.section_icon)
        val sectionTitle = sectionCard.findViewById<TextView>(R.id.section_title)
        val expandIcon = sectionCard.findViewById<ImageView>(R.id.expand_icon)
        val contentContainer = sectionCard.findViewById<LinearLayout>(R.id.content_container)
        val sectionContent = sectionCard.findViewById<TextView>(R.id.section_content)
        val divider = sectionCard.findViewById<View>(R.id.divider)
        val sectionHeader = sectionCard.findViewById<LinearLayout>(R.id.section_header)

        // Configure the section
        sectionIcon.setImageResource(iconResId)
        sectionTitle.text = title
        sectionContent.text = formatMedicationInfo(content)

        // Set initial expanded state
        contentContainer.visibility = if (expandedByDefault) View.VISIBLE else View.GONE
        divider.visibility = if (expandedByDefault) View.VISIBLE else View.GONE
        expandIcon.rotation = if (expandedByDefault) 180f else 0f

        // Set up click listener to expand/collapse
        sectionHeader.setOnClickListener {
            val isExpanded = contentContainer.visibility == View.VISIBLE

            // Toggle visibility with animation
            if (isExpanded) {
                contentContainer.visibility = View.GONE
                divider.visibility = View.GONE
                expandIcon.animate().rotation(0f).duration = 200
            } else {
                contentContainer.visibility = View.VISIBLE
                divider.visibility = View.VISIBLE
                expandIcon.animate().rotation(180f).duration = 200
            }
        }

        // Add the card to the container
        binding.infoContainer.addView(sectionCard)
    }

    private fun formatMedicationInfo(text: String): String {
        // Break text into paragraphs first
        val paragraphs = text.split("\n\n", "\r\n\r\n")
        val formattedText = StringBuilder()

        for (paragraph in paragraphs) {
            // Skip empty paragraphs
            if (paragraph.trim().isEmpty()) continue

            // Process each paragraph into bullet points if needed
            val cleanParagraph = paragraph.replace("\n", " ").trim()

            // For paragraphs that appear to be lists (with multiple sentences)
            if (cleanParagraph.contains(". ") && cleanParagraph.length > 100) {
                val sentences = cleanParagraph.split(". ")
                for (sentence in sentences) {
                    if (sentence.trim().isEmpty()) continue

                    val bulletPoint = sentence.trim() + if (sentence.endsWith(".")) "" else "."
                    formattedText.append("• $bulletPoint\n")
                }
                formattedText.append("\n")
            } else {
                // Keep short paragraphs intact
                formattedText.append(cleanParagraph)
                formattedText.append("\n\n")
            }
        }

        return formattedText.toString().trim()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}