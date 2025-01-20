package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.MedicationInfo
import com.mints.mobilehealthapplication.databinding.FragmentHomeBinding
import com.mints.mobilehealthapplication.recyclerviews.MedicationRecyclerView
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel
import com.mints.mobilehealthapplication.viewmodels.HomeFragmentViewModel

/**
 * HomeFragment displays the list of medications and serves as the main screen of the application.
 * It uses a ViewModel to fetch and observe medication data and manages the UI using ViewBinding.
 */
class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeFragmentViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    /**
     * Inflates the fragment layout using ViewBinding.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Initializes UI components, sets up RecyclerView, and fetches medication data after the view is created.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HomeFragmentViewModel::class.java]

        val mainActivity = requireActivity() as MainActivity
        mainActivity.showAllUI()
        setupFAB()
        setUpRecyclerView()

        // Fetch medications for the authenticated user
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d("HomeFragment", "Current user UID: $uid")
        if (uid.isEmpty()) {
            Log.e("HomeFragment", "User is not authenticated")
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.getMedications(uid)
        }
    }

    /**
     * Sets up the RecyclerView to display a list of medications.
     * Observes LiveData from the ViewModel to update the medication list dynamically.
     */
    private fun setUpRecyclerView() {
        Log.d("HomeFragment", "Setting up RecyclerView")

        // Initialize the RecyclerView adapter
        val adapter = MedicationRecyclerView(emptyList()) { medication ->
            viewModel.onMedicationClicked(medication)
            if (medication.notes.isNotEmpty()) {
                // Show notes in a bottom sheet if available
                val addMedicationNotesBottomSheet = MedicationNotesBottomSheet.newInstance(medication.notes)
                addMedicationNotesBottomSheet.show(parentFragmentManager, "MedicationNotesBottomSheet")
            } else {
                // Show a Snackbar message if no notes are available
                displayMessage(requireView(), "No notes available for ${medication.name}")
            }
        }

        // Attach the adapter and layout manager to the RecyclerView
        binding.medicationsRecyclerView.adapter = adapter
        binding.medicationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe medication list from the ViewModel and update the adapter
        viewModel.medications.observe(viewLifecycleOwner) { medications ->
            Log.d("HomeFragment", "Observed ${medications.size} medications in LiveData")
            adapter.updateMedicationList(medications)
        }

        Log.d("HomeFragment", "RecyclerView setup complete")
    }

    /**
     * Sets up the Floating Action Button (FAB) to navigate to the AddMedicationFragment.
     */
    private fun setupFAB() {
        val fab = requireActivity().findViewById<ExtendedFloatingActionButton>(R.id.add_medication_fab)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addMedicationBasicInfoFragment)
        }
    }

    /**
     * Updates the card displaying the next medication to be taken.
     * @param medication The medication to display in the next card.
     */
    private fun updateNextMedicationCard(medication: MedicationInfo?) {
        // Logic for updating the next medication card goes here
    }

    /**
     * Displays details of the selected medication using a Snackbar.
     * @param medication The medication whose details are to be displayed.
     */
    private fun showMedicationDetails(medication: MedicationInfo) {
        // For now, just show details in a Snackbar
    }

    /**
     * Displays a message in a Snackbar.
     * @param view The view to anchor the Snackbar to.
     */
    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Cleans up ViewBinding to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        val viewModel: AddMedicationViewModel by activityViewModels()
        viewModel.resetAllData()
        _binding = null
    }
}
