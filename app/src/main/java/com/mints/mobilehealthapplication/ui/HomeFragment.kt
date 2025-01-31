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
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.databinding.FragmentHomeBinding
import com.mints.mobilehealthapplication.recyclerviews.MedicationRecyclerView
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel
import com.mints.mobilehealthapplication.viewmodels.HomeFragmentViewModel
import java.time.ZoneId


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
    private lateinit var notificationHelper: NotificationHelper

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
        notificationHelper = NotificationHelper(requireContext())
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
                     val nextDueTimeMillis = schedule.nextDueDates[0]
                         .atZone(ZoneId.systemDefault())  // Use device's timezone
                         .toInstant()
                         .toEpochMilli()
                     notificationHelper.scheduleNotification(medication.name, medication.dosage,nextDueTimeMillis)
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


    private fun addSwipeFunctionality() {
        val swipeCallback = MaterialSwipeCallback(
            context = requireContext(),
            swipeLeftAction = MaterialSwipeCallback.SwipeAction(
                iconRes = R.drawable.baseline_close_24,
                backgroundColorRes = android.R.color.darker_gray,
                label = "Skip Medication"
            ),
            swipeRightAction = MaterialSwipeCallback.SwipeAction(
                iconRes = R.drawable.baseline_check_24px,
                backgroundColorRes = R.color.darker_green_primary_button,
                label = "Mark as Taken"

            ),
            onSwipeLeft = { position ->
                val medication = adapter.getMedicationAt(position)
                displayMessage("Mark ${medication.name} as skipped")
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
                        viewModel.markMedicationAsTaken(uid,medication)
                        if(medication.schedule is MedicationSchedule.WeeklySchedule) {
                            viewModel.testDateAdvanceMedication(uid,medication)
                        }
                        adapter.updateMedicationList(currentList)
                        adapter.notifyItemChanged(position)
                    }
                }
            })
            .show()
    }

    private fun showUndoSkipMedicationSnackbar(medication: Medication,position: Int) {
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
                        viewModel.markMedicationAsTaken(uid,medication)
                        if(medication.schedule is MedicationSchedule.WeeklySchedule) {
                            viewModel.testDateAdvanceMedication(uid,medication)
                        }
                        adapter.updateMedicationList(currentList)
                        adapter.notifyItemChanged(position)
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
        Log.d("HomeFrag","Resetting all data.")
        viewModel.resetAllData()
        viewModel.resetValidationState()
        _binding = null
    }
}
