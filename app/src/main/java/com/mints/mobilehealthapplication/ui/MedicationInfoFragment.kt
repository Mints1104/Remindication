package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.card.MaterialCardView
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.MedicationResult
import com.mints.mobilehealthapplication.databinding.FragmentTestBinding
import com.mints.mobilehealthapplication.viewmodels.MedicationInfoViewModel

class MedicationInfoFragment : Fragment() {
    private var _binding: FragmentTestBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MedicationInfoViewModel by viewModels()

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
        setupObservers()

        val medicationName = arguments?.getString("MEDICATION_NAME")
        if (medicationName.isNullOrEmpty()) {
            showError("No medication name provided")
        } else {
            mainActivity.updateToolBarTitle("Medication Info for $medicationName")
            viewModel.fetchMedicationInfo(medicationName)
        }
    }

    private fun setUpUI() {
        mainActivity.showBottomNav()
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MedicationInfoViewModel.UiState.Loading -> {
                    binding.loadingIndicator.visibility = View.VISIBLE
                    binding.infoContainer.visibility = View.GONE
                    binding.errorMessage.visibility = View.GONE
                    binding.medicationHeaderCard.visibility = View.GONE
                }

                is MedicationInfoViewModel.UiState.Success -> {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.infoContainer.visibility = View.VISIBLE
                    binding.medicationHeaderCard.visibility = View.VISIBLE
                    displayMedicationInfo(state.medicationResult)
                }

                is MedicationInfoViewModel.UiState.Error -> {
                    showError(state.message)
                }
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
        binding.infoContainer.removeAllViews()

        val brandName = result.openfda?.brand_name?.firstOrNull() ?: "Unknown"
        val genericName = result.openfda?.generic_name?.firstOrNull() ?: "Unknown"

        binding.medicationNameHeader.text = brandName
        binding.medicationGenericName.text = genericName

        addInfoSection(R.drawable.ic_directions, "How to Use",
            result.dosage_and_administration?.firstOrNull(), true)

        addInfoSection(R.drawable.ic_warning, "Warnings & Precautions",
            result.warnings?.firstOrNull(), true)

        addInfoSection(R.drawable.ic_uses, "Uses",
            result.indications_and_usage?.firstOrNull(), true)

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
        if (content.isNullOrBlank()) return

        val inflater = LayoutInflater.from(requireContext())
        val sectionCard = inflater.inflate(
            R.layout.medication_info_card,
            binding.infoContainer,
            false
        ) as MaterialCardView

        val sectionIcon = sectionCard.findViewById<ImageView>(R.id.section_icon)
        val sectionTitle = sectionCard.findViewById<TextView>(R.id.section_title)
        val expandIcon = sectionCard.findViewById<ImageView>(R.id.expand_icon)
        val contentContainer = sectionCard.findViewById<LinearLayout>(R.id.content_container)
        val sectionContent = sectionCard.findViewById<TextView>(R.id.section_content)
        val divider = sectionCard.findViewById<View>(R.id.divider)
        val sectionHeader = sectionCard.findViewById<LinearLayout>(R.id.section_header)

        sectionIcon.setImageResource(iconResId)
        sectionTitle.text = title
        sectionContent.text = viewModel.formatMedicationInfo(content)

        contentContainer.visibility = if (expandedByDefault) View.VISIBLE else View.GONE
        divider.visibility = if (expandedByDefault) View.VISIBLE else View.GONE
        expandIcon.rotation = if (expandedByDefault) 180f else 0f

        sectionHeader.setOnClickListener {
            val isExpanded = contentContainer.visibility == View.VISIBLE

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

        binding.infoContainer.addView(sectionCard)
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