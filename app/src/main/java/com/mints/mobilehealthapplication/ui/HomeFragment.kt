package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.MedicationInfo
import com.mints.mobilehealthapplication.recyclerviews.MedicationRecyclerView
import com.mints.mobilehealthapplication.viewmodels.HomeFragmentViewModel

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
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
        val mainActivity = requireActivity() as MainActivity
        mainActivity.showAppBar()
        mainActivity.showBottomNav()
        mainActivity.showFAB()
        setupFAB()
        setUpRecyclerView()

        // Fetch medications for the current user
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d("HomeFragment", "Current user UID: $uid")
        if (uid.isEmpty()) {
            Log.e("HomeFragment", "User is not authenticated")
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.getMedications(uid)
        }
    }

    private fun setUpRecyclerView() {
        Log.d("HomeFragment", "Setting up RecyclerView")

        // Initialize the adapter with an empty list
        val adapter = MedicationRecyclerView(emptyList())

        // Set the adapter to the RecyclerView
        recyclerView.adapter = adapter

        // Set a LayoutManager (e.g., LinearLayoutManager)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe the medications LiveData from the ViewModel
        viewModel.medications.observe(viewLifecycleOwner) { medications ->
            Log.d("HomeFragment", "Observed ${medications.size} medications in LiveData")
            adapter.updateMedicationList(medications)
        }

        Log.d("HomeFragment", "RecyclerView setup complete")
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


}