// MedicationHistoryFragment.kt
package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.databinding.FragmentMedicationhistoryBinding
import com.mints.mobilehealthapplication.recyclerviews.MedicationHistoryRecyclerView
import com.mints.mobilehealthapplication.viewmodels.MedicationHistoryViewModel
import java.math.BigDecimal
import java.math.RoundingMode

class MedicationHistoryFragment : Fragment() {

    private var _binding: FragmentMedicationhistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MedicationHistoryViewModel by activityViewModels()
    private lateinit var adapter: MedicationHistoryRecyclerView
    private val tag = "MedHistory"
    private var uid = ""
    private var medicationList = MutableLiveData<List<Medication>>()
    private var meds = mutableListOf<Medication>()
    private val mainActivity: MainActivity by lazy {
        requireActivity() as MainActivity
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicationhistoryBinding.inflate(inflater, container, false)
        setUpRecyclerView()
        fetchUserMedication()
        binding.visualiseAdherenceButton.setOnClickListener {
            val navController = findNavController()
            if(navController.currentDestination?.id == R.id.medicationHistoryFragment) {
                navController.navigate(R.id.action_medicationHistoryFragment_to_medicationTrendsFragment)
            }

        }

        setUpUI()
        return binding.root
    }

    private fun setUpUI() {
        mainActivity.showBottomNav()
    }

    private fun setUpRecyclerView() {
        adapter = MedicationHistoryRecyclerView(emptyList()) { medication ->
            val medId = medication.id
            if (medId != null) {
                val action = MedicationHistoryFragmentDirections
                    .actionMedicationHistoryFragmentToMedicationDetailFragment(medId)
                findNavController().navigate(action)
            } else {
                Snackbar.make(binding.root, "Medication ID is missing", Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.historyRecyclerView.adapter = adapter
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        viewModel.medications.observe(viewLifecycleOwner) { medications ->
            val filteredList = medications.filter { med -> !med.medicationHistory.isEmpty() }
            val sortedList = filteredList.sortedBy { med -> med.name.lowercase() }
            adapter.updateMedicationList(sortedList)
            val (totalTaken, totalMissed, totalSkipped) = filteredList.fold(Triple(0, 0, 0)) { acc, med ->
                Triple(
                    acc.first + med.medicationHistory.getEventCount(MedicationEvent.EventType.TAKEN),
                    acc.second + med.medicationHistory.getEventCount(MedicationEvent.EventType.MISSED),
                    acc.third + med.medicationHistory.getEventCount(MedicationEvent.EventType.SKIPPED)
                )
            }
            binding.takenCountText.text = "Taken: $totalTaken"
            binding.missedCountText.text = "Missed: $totalMissed"
            binding.skippedCountText.text = "Skipped: $totalSkipped"
            val complianceRate = viewModel.getComplianceRate()
            val rounded = BigDecimal(complianceRate).setScale(2, RoundingMode.HALF_UP).toDouble()
            binding.complianceRateText.text = getString(R.string.compliance_rate_value, rounded)
        }
    }

    private fun fetchUserMedication() {
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (uid.isEmpty()) {
            Snackbar.make(binding.root, "User not authenticated", Snackbar.LENGTH_SHORT).show()
        } else {
            viewModel.getMedications(uid)
            medicationList = viewModel.getMedicationList()




        }
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
