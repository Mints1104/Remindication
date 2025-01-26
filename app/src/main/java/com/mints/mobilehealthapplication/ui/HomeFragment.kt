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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationInfo
import com.mints.mobilehealthapplication.data.MedicationSchedule
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
    private var uid = ""
    private var medicationList = MutableLiveData<List<Medication>>()
    private val addMedicationViewModel: AddMedicationViewModel by activityViewModels()
    private lateinit var medToClear: Medication

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
        viewModel.getCurrentDay()
        addMedicationViewModel.testWeeklyDateCalculation()
        addMedicationViewModel.testWeeklyDateCalculationEdgeCase()
        addMedicationViewModel.testSingleDateAdvance()
//
//        binding.undoButton.setOnClickListener{
//            val firstMed = viewModel.medications.value?.firstOrNull()
//            if(firstMed != null) {
//                val updatedMed = viewModel.markWithUndoPrep(firstMed)
//                Log.d("UNDO_TEST", "Current stored: ${viewModel.lastOriginalDates}")
//                viewModel._medications.value = viewModel._medications.value?.map {
//                    if (it.id == firstMed.id) updatedMed else it
//                }
//
//            }
//        }

//        binding.undoButton.setOnClickListener {
//            viewModel.undoLastTaken()
//        }
//        binding.clearUndoButton.setOnClickListener {
//
//                if(medToClear != null) {
//                    viewModel.undoLastTaken()
//
//                }
//        }


    }






    private fun fetchUserMedication() {
         uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d("HomeFragment", "Current user UID: $uid")
        if (uid.isEmpty()) {
            Log.e("HomeFragment", "User is not authenticated")
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.getMedications(uid)
            medicationList = viewModel.getMedicationList()

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
             when (val schedule = medication.schedule) {
                 is MedicationSchedule.Daily -> {

                     Log.d("MED_TEST", "Times: ${schedule.times}")
                     Log.d("MED_TEST", "Calculated Dates: ${schedule.nextDueDates}")
                 }

                 is MedicationSchedule.WeeklySchedule -> {
                     Log.d("MED_TEST", "Times: ${schedule.times}")
                     Log.d("MED_TEST", "Calculated Dates: ${schedule.nextDueDates}")
                 }
                 else -> Log.d(tag,"N/A")
                 }


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
                val medicationName = adapter.getMedicationNameAt(position)
                val medication= adapter.getMedicationAt(position)
                displayMessage("Delete medication: $medicationName")
                showUndoSnackbar(medication, position)
            },
            onSwipeRight = { position ->
                val medication = adapter.getMedicationAt(position)
                displayMessage("Mark ${medication.name} as taken")
                showUndoMedicationSnackbar(medication,position)
                adapter.notifyItemChanged(position)

            }
        )

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.medicationsRecyclerView)
    }
    private fun showUndoSnackbar(medication: Medication, position: Int) {
        // Create a copy of the current list
        val currentList = adapter.getMedicationList().toMutableList()
        val removedItem = currentList.removeAt(position)

        // Update adapter with new list using DiffUtil
        adapter.updateMedicationList(currentList)

        Snackbar.make(binding.root, "${medication.name} deleted", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                // Re-insert at original position
                currentList.add(position, removedItem)
                adapter.updateMedicationList(currentList)
                adapter.notifyItemChanged(position)

            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) {
                        // Delete from ViewModel after confirmation
                        medication.id?.let {
                            viewModel.deleteMedication(uid, it) {
                                viewModel.getMedications(uid)
                            }
                        }
                    }
                }
            })
            .show()
    }

    private fun showUndoMedicationSnackbar(medication: Medication,position: Int) {
        val currentList = adapter.getMedicationList().toMutableList()

        Snackbar.make(binding.root,"${medication.name} taken", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                viewModel.undoLastTaken(medication)
                adapter.updateMedicationList(currentList)
                adapter.notifyItemChanged(position)

            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) {
                        // update firestore with new date

                        viewModel.markMedicationAsTaken(uid,medication)

                    }
                }
            })
            .show()
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
