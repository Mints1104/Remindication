package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.databinding.FragmentMedicationDetailBinding
import com.mints.mobilehealthapplication.recyclerviews.MedicationEventAdapter
import com.mints.mobilehealthapplication.viewmodels.MedicationHistoryViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MedicationDetailFragment : Fragment() {

    private var _binding: FragmentMedicationDetailBinding? = null
    private val binding get() = _binding!!
    private val args: MedicationDetailFragmentArgs by navArgs()
    private val viewModel: MedicationHistoryViewModel by activityViewModels()

    // Adapter for medication history events
    private lateinit var eventAdapter: MedicationEventAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Retrieve the medication ID from Safe Args
        val medicationId = args.medicationId

        // Set up the RecyclerView for events
        eventAdapter = MedicationEventAdapter(emptyList())
        binding.eventsRecyclerView.apply {
            adapter = eventAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Observe medications from the shared ViewModel
        viewModel.medications.observe(viewLifecycleOwner) { medicationList ->
            val med = medicationList.find { it.id == medicationId }
            med?.let {
                val mainActivity = requireActivity() as MainActivity
                mainActivity.updateToolBarTitle(med.name)


              val complianceRate =   it.medicationHistory.getComplianceRate()
                val rounded = BigDecimal(complianceRate).setScale(2,RoundingMode.HALF_UP).toDouble()
                // Update adherence summary using actual data
                binding.complianceRateText.text = getString(R.string.compliance_rate_value, rounded)
                binding.takenCountText.text = "Taken: ${it.medicationHistory.getEventCount(MedicationEvent.EventType.TAKEN)}"
                binding.missedCountText.text = "Missed: ${it.medicationHistory.getEventCount(MedicationEvent.EventType.MISSED)}"
                binding.skippedCountText.text = "Skipped: ${it.medicationHistory.getEventCount(MedicationEvent.EventType.SKIPPED)}"

                // Update the events adapter with all history events
                eventAdapter.updateData(it.medicationHistory.events.reversed())
            }
        }
    }

    fun formatMedicationEventDate(dateTime: LocalDateTime): String {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        return when (dateTime.toLocalDate()) {
            today -> "Today at ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            yesterday -> "Yesterday at ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            else -> dateTime.format(DateTimeFormatter.ofPattern("MMM d at h:mm a"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
