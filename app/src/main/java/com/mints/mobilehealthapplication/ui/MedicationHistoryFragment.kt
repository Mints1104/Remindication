package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.databinding.FragmentMedicationhistoryBinding
import com.mints.mobilehealthapplication.recyclerviews.MedicationHistoryRecyclerView
import com.mints.mobilehealthapplication.viewmodels.MedicationHistoryViewModel


class MedicationHistoryFragment : Fragment() {
    private var _binding: FragmentMedicationhistoryBinding? = null
    private val viewModel: MedicationHistoryViewModel by activityViewModels()
    private lateinit var adapter: MedicationHistoryRecyclerView
    private val tag = "MedHistory"
    private var uid = ""
    private var medicationList = MutableLiveData<List<Medication>>()
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicationhistoryBinding.inflate(inflater, container, false)


        setUpRecyclerView()
        fetchUserMedication()
        return binding.root
    }


    private fun displayMessage(msgTxt: String) {
        Snackbar.make(binding.root, msgTxt, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    private fun setUpRecyclerView() {
        Log.d(tag, "Setting up RecyclerView")

        adapter = MedicationHistoryRecyclerView(emptyList()) { medication ->
            viewModel.onMedicationClicked(medication)
            when (val schedule = medication.schedule) {
                is MedicationSchedule.Daily -> {

                    Log.d(tag, "Times: ${schedule.times}")
                    Log.d(tag, "Calculated Dates: ${schedule.nextDueDates}")

                    viewModel.testReceivingMedicationHistory(medication)

                }

                is MedicationSchedule.WeeklySchedule -> {
                    Log.d(tag, "Times: ${schedule.times}")
                    Log.d(tag, "Calculated Dates: ${schedule.nextDueDates}")
                    viewModel.testReceivingMedicationHistory(medication)

                }
                else -> Log.d(tag,"N/A")
            }


            if (!medication.medicationHistory.isEmpty()) {
                val addMedicationNotesBottomSheet = MedicationNotesBottomSheet.newInstance(medication.notes)
                addMedicationNotesBottomSheet.show(parentFragmentManager, "MedicationNotesBottomSheet")

            } else {
                displayMessage("No medication history available for ${medication.name}")
            }
        }

        binding.historyRecyclerView.adapter = adapter
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        viewModel.medications.observe(viewLifecycleOwner) { medications ->
            Log.d(tag, "Observed ${medications.size} medications in LiveData")
            val filteredList = medications.filter { medication -> !medication.medicationHistory.isEmpty() }


            adapter.updateMedicationList(filteredList)
        }

        Log.d(tag, "RecyclerView setup complete")
    }

    private fun fetchUserMedication() {
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d(tag, "Current user UID: $uid")
        if (uid.isEmpty()) {
            Log.e(tag, "User is not authenticated")
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.getMedications(uid)
            medicationList = viewModel.getMedicationList()

        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
