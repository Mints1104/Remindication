package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.HomeViewModel
import com.mints.mobilehealthapplication.data.MedicationInfo

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var nextMedicationName: TextView
    private lateinit var nextMedicationTime: TextView
    private lateinit var loadingIndicator: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.medications_recycler_view)
        nextMedicationName = view.findViewById(R.id.next_medication_name)
        nextMedicationTime = view.findViewById(R.id.next_medication_time)
        loadingIndicator = view.findViewById(R.id.loading_indicator)
        val mainActivity = requireActivity() as MainActivity
        mainActivity.showAppBar()
        mainActivity.showBottomNav()
        mainActivity.showFAB()
        setupFAB()
        setupViewModel()

    }

    private fun setupViewModel() {
        viewModel.searchResult.observe(viewLifecycleOwner) { result ->
            loadingIndicator.visibility = View.GONE

            result.onSuccess { message ->
                displayMessage(requireView(), message)
            }.onFailure { exception ->
                displayMessage(requireView(), "Error: ${exception.message}")
            }
        }
    }





    private fun setupFAB() {
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.add_medication_fab)
        fab.setOnClickListener {
            showAddMedicationDialog()
        }
    }

    private fun updateNextMedicationCard(medication: MedicationInfo?) {
        // Your logic for updating the next medication card
    }

    private fun showMedicationDetails(medication: MedicationInfo) {
        // For now, just show details in a Snackbar
    }


    private fun showAddMedicationDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter medication name"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Search Medication")
            .setView(editText)
            .setPositiveButton("Search") { _, _ ->
                loadingIndicator.visibility = View.VISIBLE
                viewModel.searchMedication(editText.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun displayMessage(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).apply {
            setAction("Dismiss") { dismiss() }
            show()
        }
    }

    override fun onResume() {
        super.onResume()
    }


}
