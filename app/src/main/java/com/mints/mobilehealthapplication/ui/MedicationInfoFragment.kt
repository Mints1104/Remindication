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
import com.mints.mobilehealthapplication.data.MedicationResult
import com.mints.mobilehealthapplication.data.RetrofitClient
import com.mints.mobilehealthapplication.databinding.FragmentTestBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class MedicationInfoFragment : Fragment() {
    private var _binding: FragmentTestBinding? = null
    private val binding get() = _binding!!

    private var medicationName: String? = null
    private val mainActivity: MainActivity by lazy {
        requireActivity() as MainActivity
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpUI()
        medicationName = arguments?.getString("MEDICATION_NAME")
        if (medicationName.isNullOrEmpty()) {
            showError("No medication name provided")
        } else {
            mainActivity.updateToolBarTitle("Medication Info for ${medicationName!!}")
            fetchMedicationInfo(medicationName!!)
        }
    }

    private fun setUpUI() {
        mainActivity.showBottomNav()
    }

    private fun fetchMedicationInfo(drugName: String) {
        // Show loading state
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.infoContainer.visibility = View.GONE
        binding.errorMessage.visibility = View.GONE
        binding.medicationHeaderCard.visibility = View.GONE

        lifecycleScope.launch {
            try {
                Log.d("TestFragment", "Starting fetch for: $drugName")
                val searchQuery = "openfda.brand_name:$drugName"

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.fdaApi.getMedicationInfo(searchQuery)
                }

                if (response.results.isNotEmpty()) {
                    val result = response.results[0]
                    binding.loadingIndicator.visibility = View.GONE
                    binding.infoContainer.visibility = View.VISIBLE
                    binding.medicationHeaderCard.visibility = View.VISIBLE

                    displayMedicationInfo(result)
                } else {
                    showError("No information found for $drugName")
                }
            } catch (e: HttpException) {
                Log.e("TestFragment", "HTTP error: ${e.code()}", e)
                val errorMessage = if (e.code() == 404) {
                    "No information found for $drugName"
                } else {
                    "Unable to retrieve information. Please try again later."
                }
                showError(errorMessage)
            } catch (e: Exception) {
                Log.e("TestFragment", "Error fetching medication info", e)
                showError("Error fetching data: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        if(_binding == null) return
        binding.loadingIndicator.visibility = View.GONE
        binding.infoContainer.visibility = View.GONE
        binding.medicationHeaderCard.visibility = View.GONE
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = message
    }

    private fun displayMedicationInfo(result: MedicationResult) {
        // Clear any existing content
        binding.infoContainer.removeAllViews()

        // Display medication name and generic name in the header
        val brandName = result.openfda?.brand_name?.firstOrNull() ?: "Unknown"
        val genericName = result.openfda?.generic_name?.firstOrNull() ?: "Unknown"

        binding.medicationNameHeader.text = brandName
        binding.medicationGenericName.text = genericName

        // Key medication information sections (expanded by default)
        addInfoSection(R.drawable.ic_directions, "How to Use",
            result.dosage_and_administration?.firstOrNull(), true)

        addInfoSection(R.drawable.ic_warning, "Warnings & Precautions",
            result.warnings?.firstOrNull(), true)

        addInfoSection(R.drawable.ic_uses, "Uses",
            result.indications_and_usage?.firstOrNull(), true)

        // Secondary information (collapsed by default)
        addInfoSection(R.drawable.ic_ingredient, "Active Ingredients",
            result.active_ingredient?.firstOrNull(), false)

        addInfoSection(R.drawable.ic_stop, "When to Stop Use",
            result.stop_use?.firstOrNull(), false)

        addInfoSection(R.drawable.ic_warning, "Do Not Use If",
            result.do_not_use?.firstOrNull(), false)

        addInfoSection(R.drawable.ic_ask_doctor, "Ask Doctor",
            result.ask_doctor?.firstOrNull(), false)

        addInfoSection(R.drawable.ic_pregnancy, "Pregnancy or Breastfeeding",
            result.pregnancy_or_breast_feeding?.firstOrNull(), false)

        addInfoSection(R.drawable.ic_storage, "Storage Information",
            result.storage_and_handling?.firstOrNull(), false)

        addInfoSection(R.drawable.ic_ingredient, "Inactive Ingredients",
            result.inactive_ingredient?.firstOrNull(), false)
    }

    private fun addInfoSection(iconResId: Int, title: String, content: String?, expandedByDefault: Boolean) {
        // Skip if content is null or empty
        if (content.isNullOrBlank()) return

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

    override fun onResume() {
        super.onResume()
        setUpUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}