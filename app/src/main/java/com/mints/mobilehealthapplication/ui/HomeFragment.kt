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
import androidx.recyclerview.widget.ItemTouchHelper
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
    private lateinit var adapter: MedicationRecyclerView

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
        fetchUserMedication()
    }

    private fun fetchUserMedication() {
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

         adapter = MedicationRecyclerView(emptyList()) { medication ->
            viewModel.onMedicationClicked(medication)
            if (medication.notes.isNotEmpty()) {
                val addMedicationNotesBottomSheet = MedicationNotesBottomSheet.newInstance(medication.notes)
                addMedicationNotesBottomSheet.show(parentFragmentManager, "MedicationNotesBottomSheet")
            } else {
                displayMessage("No notes available for ${medication.name}")
            }
        }

        binding.medicationsRecyclerView.adapter = adapter
        binding.medicationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        addSwipeFunctionality()
        viewModel.medications.observe(viewLifecycleOwner) { medications ->
            Log.d("HomeFragment", "Observed ${medications.size} medications in LiveData")
            adapter.updateMedicationList(medications)
        }

        Log.d("HomeFragment", "RecyclerView setup complete")
    }

//    private fun addSwipeFunctionality() {
//        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
//            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
//                return false
//            }
//
//            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                val position = viewHolder.adapterPosition
//                when (direction) {
//                    ItemTouchHelper.LEFT -> {
//
//                        displayMessage("Swiped left")
//
//                    }
//                    ItemTouchHelper.RIGHT -> {
//                        displayMessage("Swiped right")
//                    }
//
//                }
//                adapter.notifyItemChanged(position)
//
//            }
//        })
//        itemTouchHelper.attachToRecyclerView(binding.medicationsRecyclerView)
//    }

    private fun addSwipeFunctionality() {
        val swipeCallback = MaterialSwipeCallback(
            context = requireContext(),
            swipeLeftAction = MaterialSwipeCallback.SwipeAction(
                iconRes = R.drawable.baseline_delete_24px,
                backgroundColorRes = android.R.color.holo_red_dark,
                label = "Delete Medication"
            ),
            swipeRightAction = MaterialSwipeCallback.SwipeAction(
                iconRes = R.drawable.baseline_check_24px,
                backgroundColorRes = R.color.darker_green_primary_button,
                label = "Mark as Taken"
            ),
            onSwipeLeft = { position ->
                // Handle delete action
                displayMessage("Delete medication")
                adapter.notifyItemChanged(position)
            },
            onSwipeRight = { position ->
                // Handle mark as taken action
                displayMessage("Marked as taken")
                adapter.notifyItemChanged(position)
            }
        )

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.medicationsRecyclerView)
    }






    /**
     * Sets up the Floating Action Button (FAB) to navigate to the AddMedicationBasicInfoFragment.
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
     * Displays a message in a Snackbar at the bottom of the screen.
     */
    private fun displayMessage(msgTxt: String) {
        Snackbar.make(binding.root, msgTxt, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()

    }

    /**
     * Cleans up ViewBinding to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        val viewModel: AddMedicationViewModel by activityViewModels()
        viewModel.resetAllData()
        viewModel.resetValidationState()
        _binding = null
    }
}
