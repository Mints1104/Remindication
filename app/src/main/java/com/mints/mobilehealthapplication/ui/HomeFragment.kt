package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.MedicationInfo
import com.mints.mobilehealthapplication.recyclerviews.MedicationRecyclerView
import com.mints.mobilehealthapplication.viewmodels.HomeFragmentViewModel

class HomeFragment : Fragment() {

    //private val viewModel: HomeViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var nextMedicationName: TextView
    private lateinit var nextMedicationTime: TextView
    private lateinit var loadingIndicator: View
    private lateinit var viewModel: HomeFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HomeFragmentViewModel::class.java]

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
       // setupViewModel()

    }

    /*
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

     */

    private fun setUpRecyclerView() {
        // Initialize the adapter with an empty list
        val adapter = MedicationRecyclerView(emptyList())

        // Set the adapter to the RecyclerView
        recyclerView.adapter = adapter

        // Observe the medications LiveData from the ViewModel
        viewModel.medications.observe(viewLifecycleOwner, Observer { medications ->
            // Update the adapter's list when the data changes
            medications?.let {
                adapter.updateMedicationList(it)
            }
        })
    }





    private fun setupFAB() {
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.add_medication_fab)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addMedicationFragment)

        }
    }

    private fun updateNextMedicationCard(medication: MedicationInfo?) {
        // Your logic for updating the next medication card
    }

    private fun showMedicationDetails(medication: MedicationInfo) {
        // For now, just show details in a Snackbar
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