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
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationInfo
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.databinding.FragmentHomeBinding
import com.mints.mobilehealthapplication.recyclerviews.MedicationRecyclerView
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel
import com.mints.mobilehealthapplication.viewmodels.HomeFragmentViewModel
import com.mints.mobilehealthapplication.viewmodels.HomeFragmentViewModelFactory
import java.time.LocalDateTime
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

    private fun showLoadingState() {
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.nextMedicationCard.visibility = View.INVISIBLE
    }

    private fun showContent() {
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        binding.nextMedicationCard.visibility = View.VISIBLE
    }

    /**
     * Initializes UI components, sets up RecyclerView, and fetches medication data after the view is created.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationHelper = NotificationHelper(requireContext())

        val factory = HomeFragmentViewModelFactory(notificationHelper)
        viewModel = ViewModelProvider(this, factory)[HomeFragmentViewModel::class.java]
        val mainActivity = requireActivity() as MainActivity
        mainActivity.showAllUI()
        setupFAB()
        setUpRecyclerView()
        showLoadingState()

        fetchUserMedication()
        viewModel.getCurrentDay()
    }


    private fun fetchUserMedication() {
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d("HomeFragment", "Current user UID: $uid")
        if (uid.isEmpty()) {
            Log.e("HomeFragment", "User is not authenticated")
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            showContent() // Hide shimmer if there's an error
        } else {
            viewModel.getMedications(uid) {
                viewModel.medications.observe(viewLifecycleOwner) { list ->
                    if (list != null) {
                        getClosestDate(list)
                        showContent() // Show content when data is loaded
                    } else {
                        Log.d(tag, "No medications found")
                        showContent() // Hide shimmer even if no medications found
                    }
                }
            }
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

                     testReceivingMedicationHistory(medication)
                     testNotificationSchedule(medication)
                 }
                 is MedicationSchedule.WeeklySchedule -> {
                     Log.d("MED_TEST", "Times: ${schedule.times}")
                     Log.d("MED_TEST", "Calculated Dates: ${schedule.nextDueDates}")
                 }
                 else -> Log.d(tag,"N/A")
                 }
             handleDisplayingNotes(medication)
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

    private fun testNotificationSchedule(medication: Medication) {
        when(val schedule = medication.schedule) {
            is MedicationSchedule.Daily -> {
                Log.d("MED_TEST", "Times Daily: ${schedule.times}")
                Log.d("MED_TEST", "Calculated Dates Daily: ${schedule.nextDueDates}")
                val nextDueTimeMillis = schedule.nextDueDates[0]
                    .atZone(ZoneId.systemDefault())  // Use device's timezone
                    .toInstant()
                    .toEpochMilli()
                notificationHelper.scheduleNotification(
                    medication.name,
                    medication.dosage,
                    nextDueTimeMillis
                )
            }
            is MedicationSchedule.WeeklySchedule -> {
                Log.d("MED_TEST", "Times Weekly: ${schedule.times}")
                Log.d("MED_TEST", "Calculated Dates Weekly: ${schedule.nextDueDates}")
                val nextDueTimeMillis = schedule.nextDueDates[0]
                    .atZone(ZoneId.systemDefault())  // Use device's timezone
                    .toInstant()
                    .toEpochMilli()
                notificationHelper.scheduleNotification(
                    medication.name,
                    medication.dosage,
                    nextDueTimeMillis
                )
            }
            else -> Log.d(tag,"Yet to complete.")
        }
    }

    private fun testReceivingMedicationHistory(medication: Medication) {
        val history = medication.medicationHistory
        Log.d("MED_TEST", "History: $history")
        history.getLastEventOfType(MedicationEvent.EventType.TAKEN)?.let { lastTaken ->
            Log.d("MedicationHistory", "Last taken: ${lastTaken.date}")
        }

        val compliance = history.getComplianceRate()
        Log.d("MedicationHistory", "Compliance rate: $compliance%")

        if (history.wasTakenToday()) {
            Log.d("MedicationHistory", "Medication already taken today")
        }

        val recentEvents = history.getEventsFromLastDays(7)
        Log.d("MedicationHistory", "Events in last 7 days: ${recentEvents.size}")
    }

    private fun handleDisplayingNotes(medication: Medication) {
        if (medication.notes.isNotEmpty()) {
            val addMedicationNotesBottomSheet =
                MedicationNotesBottomSheet.newInstance(medication.notes)
            addMedicationNotesBottomSheet.show(parentFragmentManager, "MedicationNotesBottomSheet")

        } else {
            displayMessage("No notes available for ${medication.name}")
        }
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
                showUndoSkipMedicationSnackbar(medication,position)
                adapter.notifyItemChanged(position)
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
                        testNotificationSchedule(medication)
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

        Snackbar.make(binding.root,"${medication.name} skipped", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                viewModel.undoLastTaken(medication)
                adapter.updateMedicationList(currentList)
                adapter.notifyItemChanged(position)

            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) {
                        viewModel.markMedicationAsSkipped(uid,medication)
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

    private fun getClosestDate(currentList: List<Medication>) {
        Log.d(tag, "Starting getClosestDate function")

        Log.d(tag, "Current medication list size: ${currentList.size}")

        // Filter daily schedules
        val dailySchedList = currentList.filter { it.schedule is MedicationSchedule.Daily }
        Log.d(tag, "Filtered daily schedules: ${dailySchedList.size} medications")

        // Get the current time
        val now = LocalDateTime.now()
        Log.d(tag, "Current time: $now")

        // Find the medication with the earliest upcoming date
        val closestMedication = dailySchedList
            .filter { it.schedule is MedicationSchedule.Daily }
            .minByOrNull { medication ->
                val schedule = medication.schedule as MedicationSchedule.Daily
                val closestDate = schedule.nextDueDates
                    .filter { it.isAfter(now) }
                    .minOrNull()
                Log.d(tag, "Evaluating medication: ${medication.name}, closest due date: $closestDate")
                closestDate ?: LocalDateTime.MAX
            }

        if (closestMedication != null) {
            val schedule = closestMedication.schedule as MedicationSchedule.Daily
            val closestDueDate = schedule.nextDueDates.filter { it.isAfter(now) }.minOrNull()
            Log.d(tag, "Closest medication: ${closestMedication.name}, closest due date: $closestDueDate")

            binding.nextMedicationName.text = getString(R.string.name_of_next_med, closestMedication.name)
            binding.nextMedicationTime.text = getString(R.string.time_of_medication, closestDueDate.toString())
        } else {
            Log.d(tag, "No future due dates found.")
        }

        Log.d(tag, "Completed getClosestDate function")
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

    override fun onResume() {
        super.onResume()
        binding.shimmerLayout.startShimmer()
    }

    override fun onPause() {
        binding.shimmerLayout.stopShimmer()
        super.onPause()
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
