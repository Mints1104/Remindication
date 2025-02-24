// MedicationDetailFragment.kt
package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mints.mobilehealthapplication.databinding.FragmentMedicationDetailBinding
import com.mints.mobilehealthapplication.recyclerviews.DailyAdherenceAdapter
import com.mints.mobilehealthapplication.viewmodels.MedicationHistoryViewModel
import java.time.LocalDate

class MedicationDetailFragment : Fragment() {

    private var _binding: FragmentMedicationDetailBinding? = null
    private val binding get() = _binding!!
    private val args: MedicationDetailFragmentArgs by navArgs()
    private val viewModel: MedicationHistoryViewModel by activityViewModels()

    // Adapter for daily adherence (using Pair<LocalDate, String>)
    private lateinit var dailyAdherenceAdapter: DailyAdherenceAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Retrieve the medication ID passed via Safe Args
        val medicationId = args.medicationId

        // Set up RecyclerView for daily adherence details
        dailyAdherenceAdapter = DailyAdherenceAdapter(emptyList())
        binding.dailyAdherenceRecyclerView.apply {
            adapter = dailyAdherenceAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Observe the shared medications list from the ViewModel
        viewModel.medications.observe(viewLifecycleOwner) { medicationList ->
            val medication = medicationList.find { it.id == medicationId }
            medication?.let { med ->
//                // Populate basic medication info
//                binding.medicationName.text = med.name
//                binding.medicationDosage.text = med.dosage
//                binding.medicationSchedule.text = when (med.schedule) {
//                    is com.mints.mobilehealthapplication.data.MedicationSchedule.Daily -> "Daily: ${med.schedule.formattedFrequency}"
//                    is com.mints.mobilehealthapplication.data.MedicationSchedule.WeeklySchedule -> "Weekly: ${med.schedule.formattedFrequency}"
//                    is com.mints.mobilehealthapplication.data.MedicationSchedule.Cyclic -> "Cyclic Schedule"
//                    is com.mints.mobilehealthapplication.data.MedicationSchedule.Interval -> "Interval Schedule"
//                    is com.mints.mobilehealthapplication.data.MedicationSchedule.OnDemand -> "On-Demand"
//                }
//                binding.medicationNotes.text = med.notes

                // Use actual adherence rates from the medication history
                binding.complianceRateText.text = "Compliance Rate: ${med.medicationHistory.getComplianceRate()}%"
                binding.takenCountText.text = "Taken: ${med.medicationHistory.getEventCount(com.mints.mobilehealthapplication.data.MedicationEvent.EventType.TAKEN)}"
                binding.missedCountText.text = "Missed: ${med.medicationHistory.getEventCount(com.mints.mobilehealthapplication.data.MedicationEvent.EventType.MISSED)}"
                binding.skippedCountText.text = "Skipped: ${med.medicationHistory.getEventCount(com.mints.mobilehealthapplication.data.MedicationEvent.EventType.SKIPPED)}"

                med.medicationHistory.getAllEvents().forEach { event ->
                    Log.d("Test","Event: $event")
                }
                // Build a daily adherence list for, say, the past 7 days.
                // Replace this logic with your own adherence calculation.
                val today = LocalDate.now()
                val adherenceList = (0 until 7).map { offset ->
                    val date = today.minusDays(offset.toLong())
                    // Check if there's a "TAKEN" event on this date (modify logic as needed)
                    val status = if (med.medicationHistory.events.any { it.date.toLocalDate() == date && it.type.name == "TAKEN" }) {
                        "Taken"
                    } else {
                        "Missed"
                    }
                    Pair(date, status)
                }.reversed()  // so the earliest day appears at the top

                dailyAdherenceAdapter.updateData(adherenceList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
