    package com.mints.mobilehealthapplication.ui

    import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.MotivationManager
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.data.getNextDueDates
import com.mints.mobilehealthapplication.databinding.FragmentHomeBinding
import com.mints.mobilehealthapplication.recyclerviews.MedicationRecyclerView
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel
import com.mints.mobilehealthapplication.viewmodels.HomeFragmentViewModel
import java.time.LocalDate
import java.time.LocalDateTime
    import java.time.format.DateTimeFormatter


    /**
     * HomeFragment displays the list of medications and serves as the main screen of the application.
     * It uses a ViewModel to fetch and observe medication data and manages the UI using ViewBinding.
     */
    class HomeFragment : Fragment() {

        private var _binding: FragmentHomeBinding? = null
        private val binding get() = _binding!!
        private lateinit var adapter: MedicationRecyclerView
        private var uid = ""
        private lateinit var notificationHelper: NotificationHelper
        private var tag = "HomeFragment"
        private var deviceConnected = false
        private var scheduledMeds = mutableListOf<Medication>()
        private var onDemandMeds = mutableListOf<Medication>()
        private var fullListOfMeds = mutableListOf<Medication>()

        private val viewModel: HomeFragmentViewModel by activityViewModels(
            factoryProducer = { (requireActivity() as MainActivity).homeFragmentViewModelFactory }
        )

        private val mainActivity: MainActivity by lazy {
            requireActivity() as MainActivity
        }

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


            binding.motivationCover.setOnClickListener {
                revealMotivationContent()
                it.visibility = View.GONE
            }


            setUpUI()
            setupFAB()
            setUpRecyclerView()
            showLoadingState()
            resetIfNewDay()

            deviceConnected = isDeviceConnected()



            Log.d(tag,"Is device connected: $deviceConnected ")
            uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            Log.d(tag, "Current user UID: $uid")
            observeNetworkState()
            if (uid.isEmpty()) {
                Log.e(tag, "User is not authenticated")
                Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
                showContent()
            } else {
                viewModel.medications.observe(viewLifecycleOwner) { list ->
                    if (!list.isNullOrEmpty()) {
                        showContent()
                        val medications = getUncompletedMedicationsForToday(list)
                        medications.forEach { medication ->
                            Log.d("TestFilteredMeds", "Uncompleted med: ${medication.name}")
                        }
                    } else {
                        Log.d(tag, "No medications found")
                        showContent()
                    }
                }

                viewModel.getMedications(uid)
            }


            viewModel.getCurrentDay()
        }

        private fun setUpUI() {
            mainActivity.showAllUI()
        }


        private val refreshReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == REFRESH_ACTION) {
                    Log.d(tag, "Received refresh broadcast, updating UI")
                    viewModel.invalidateCache()
                    viewModel.getMedications(uid)
                }
            }
        }


        companion object {
            const val REFRESH_ACTION = "com.mints.mobilehealthapplication.REFRESH_MEDICATIONS"
        }

        private fun observeNetworkState() {
            mainActivity.internetChecker.connectionState.observe(viewLifecycleOwner) { isConnected ->
                deviceConnected = isConnected
            }
        }

        private fun isDeviceConnected(): Boolean {
            return mainActivity.checkNetworkState()
        }

        /**
         * Sets up the RecyclerView to display a list of medications.
         * Observes LiveData from the ViewModel to update the medication list dynamically.
         */
        private fun setUpRecyclerView() {
            Log.d(tag, "Setting up RecyclerView")
             adapter = MedicationRecyclerView(emptyList()) { medication ->
                viewModel.onMedicationClicked(medication)
                 when (val schedule = medication.schedule) {
                     is MedicationSchedule.Daily -> {



                         viewModel.testReceivingMedicationHistory(medication)
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
                Log.d(tag, "Observed ${medications.size} medications in LiveData")
                fullListOfMeds = medications.toMutableList()
                val filteredMeds = getUncompletedMedicationsForToday(medications)
                 scheduledMeds = filteredMeds.filter {
                     it.schedule !is MedicationSchedule.OnDemand
                 }.toMutableList()
                if(scheduledMeds.isNotEmpty()) {
                    binding.motivationCover.visibility = View.GONE
                }

                onDemandMeds = filteredMeds.filter { it.schedule is MedicationSchedule.OnDemand }
                    .toMutableList()

                scheduledMeds.forEach { Log.d("ScheduledMeds","Test: ${it.name}") }
                onDemandMeds.forEach { Log.d("OnDemandMeds","TestOD: ${it.name}") }
                val today = LocalDate.now()
                val sortedMeds = scheduledMeds
                    .filter { medication ->
                        medication.schedule.getNextDueDates().any { it.toLocalDate() == today }
                    }
                    .sortedBy { medication ->
                        medication.schedule.getNextDueDates()
                            .first { it.toLocalDate() == today }  // Get the first due date for today
                            .toLocalTime()  // Sort by the time
                    }

                if(sortedMeds.isEmpty() && fullListOfMeds.isNotEmpty()) {
                    handleAllMedicationsCompleted()
                }

                adapter.updateMedicationList(sortedMeds)
                adapter.hideAllMedicationDays()
                getClosestDate(scheduledMeds)
                checkDateInPast(medications)

            }




            Log.d(tag, "RecyclerView setup complete")
        }




        private fun handleDisplayingNotes(medication: Medication) {
                val addMedicationNotesBottomSheet =
                    MedicationNotesBottomSheet.newInstance(medication.name, medication.notes)
                addMedicationNotesBottomSheet.show(parentFragmentManager, "MedicationNotesBottomSheet")
            }



        private fun getUncompletedMedicationsForToday(medications: List<Medication>): List<Medication> {
            val today = LocalDate.now()

            return medications.filter { medication ->
                when (val schedule = medication.schedule) {
                    is MedicationSchedule.Daily -> {
                        // Check if any due dates are today
                        val hasDueDatesToday = schedule.nextDueDates.any { it.toLocalDate() == today }

                        if (hasDueDatesToday) {
                            val takenToday = medication.medicationHistory.events
                                .count { event ->
                                    (event.type == MedicationEvent.EventType.TAKEN ||
                                            event.type == MedicationEvent.EventType.SKIPPED) &&
                                            event.date.toLocalDate() == today
                                }

                            val requiredDoses = schedule.frequency.ordinal + 1
                            takenToday < requiredDoses
                        } else {
                            false
                        }
                    }

                    is MedicationSchedule.WeeklySchedule -> {
                        // Check if any due dates are today
                        val hasDueDatesToday = schedule.nextDueDates.any { it.toLocalDate() == today }

                        if (hasDueDatesToday) {
                            val takenToday = medication.medicationHistory.events
                                .count { event ->
                                    event.type == MedicationEvent.EventType.TAKEN &&
                                            event.date.toLocalDate() == today
                                }

                            takenToday < schedule.times.size
                        } else {
                            false
                        }
                    }
                    is MedicationSchedule.Cyclic -> {
                        val hasDueDatesToday = schedule.nextDueDates.any{it.toLocalDate() == today}
                        if(hasDueDatesToday) {
                            val takenToday = medication.medicationHistory.events.count{ event ->
                                event.type == MedicationEvent.EventType.TAKEN &&
                                        event.date.toLocalDate() == today
                            }
                            takenToday < schedule.times.size
                        } else {
                            false
                        }
                    }
                    is MedicationSchedule.OnDemand -> true

                    else -> false
                }
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
                onSwipeLeft = { position, onActionCompleted ->
                    val medication = adapter.getMedicationAt(position)
                    /*
                    if (deviceConnected) {
                        displayMessage("Mark ${medication.name} as skipped")
                        showUndoSnackbar(medication,position,true, onActionCompleted)
                    } else {
                        displayMessage("Device not connected to internet")
                        onActionCompleted()
                    }

                     */
                    showUndoSnackbar(medication,position,true, onActionCompleted)

                },
                onSwipeRight = { position, onActionCompleted ->
                    val medication = adapter.getMedicationAt(position)
                    showUndoSnackbar(medication, position,false, onActionCompleted)

                }
            )
            ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.medicationsRecyclerView)
        }

        private fun showUndoSnackbar(
            medication: Medication,
            position: Int,
            isSkipped: Boolean,
            onActionCompleted: () -> Unit
        ) {
            val oldList = adapter.getMedicationList().toMutableList()
            val targetIndex = oldList.indexOf(medication)
            if (targetIndex == -1) {
                Log.e("SwipeDebug", "Medication ${medication.name} not found in list!")
                onActionCompleted()
                return
            }

            val updatedMedication = medication.copy()
            if (isSkipped) updatedMedication.markAsSkipped(dateTime = LocalDateTime.now())
            else updatedMedication.markAsTaken(dateTime = LocalDateTime.now())
            oldList[targetIndex] = updatedMedication
            adapter.updateMedicationList(oldList)
            updateNextMedicationCard(oldList)

            Log.d("SwipeDebug", "After mark: $oldList, size: ${oldList.size}")

            val message = if (isSkipped) "${medication.name} skipped" else "${medication.name} taken"
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setAction("UNDO") {
                    val currentList = adapter.getMedicationList().toMutableList()
                    currentList[targetIndex] = medication
                    adapter.updateMedicationList(currentList)
                    updateNextMedicationCard(currentList)
                    Log.d("SwipeDebug", "After undo: $currentList, size: ${currentList.size}")
                    onActionCompleted()
                }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        if (event != DISMISS_EVENT_ACTION) {
                            medication.id?.let {
                                if (isSkipped) {
                                    viewModel.markMedicationAsSkipped(uid, updatedMedication) {
                                        Log.d("SwipeDebug", "Marked ${medication.name} as skipped in backend")
                                        onActionCompleted()
                                    }
                                } else {
                                    viewModel.markMedicationAsTaken(uid, updatedMedication) {
                                        Log.d("SwipeDebug", "Marked ${medication.name} as taken in backend")
                                        onActionCompleted()
                                    }
                                }
                            } ?: onActionCompleted()
                        } else {

                            medication.id?.let {
                                viewModel.undoLastTaken(medication)
                                val finalList = adapter.getMedicationList().toMutableList()
                                val index = finalList.indexOfFirst { it.id == medication.id }
                                if (index == -1) {
                                    if (targetIndex < finalList.size) {
                                        finalList.add(targetIndex, medication)
                                    } else {
                                        finalList.add(medication)
                                    }
                                } else {
                                    finalList[index] = medication
                                }
                                adapter.updateMedicationList(finalList)
                                updateNextMedicationCard(finalList)
                                Log.d("SwipeDebug", "Undone ${medication.name} in backend, list: $finalList, size: ${finalList.size}")
                            }
                            onActionCompleted()
                        }
                        onActionCompleted()

                    }

                })
                .show()

            adapter.notifyItemChanged(position)
        }

        private fun updateNextMedicationCard(medications: List<Medication>) {
            if (_binding == null) return

            if (medications.isEmpty()) {
                binding.nextMedicationName.text = getString(R.string.no_medications_left)
                binding.nextMedicationTime.visibility = View.GONE
                binding.motivationCover.visibility = View.VISIBLE
            } else {
                val now = LocalDateTime.now()
                val closestMedication = medications.minByOrNull { med ->
                    val nextDueDates = med.schedule.getNextDueDates()
                    val closestDate = nextDueDates.filter { it.isAfter(now) }
                        .minOrNull() ?: nextDueDates.minOrNull() ?: LocalDateTime.MAX
                    closestDate
                }

                if (closestMedication != null) {
                    val nextDueDates = closestMedication.schedule.getNextDueDates()
                    val closestDueDate = nextDueDates.filter { it.isAfter(now) }
                        .minOrNull() ?: nextDueDates.minOrNull()

                    binding.nextMedicationName.text = getString(R.string.name_of_next_med, closestMedication.name)
                    if (closestDueDate != null) {
                        val formatter = DateTimeFormatter.ofPattern("HH:mm")
                        val formattedTime = closestDueDate.format(formatter)

                        binding.nextMedicationTime.text = getString(
                            R.string.time_of_medication,
                            formattedTime
                        )
                        binding.nextMedicationTime.visibility = View.VISIBLE
                    } else {
                        binding.nextMedicationTime.visibility = View.GONE
                    }
                    binding.motivationCover.visibility = View.GONE
                } else {
                    binding.nextMedicationName.text = getString(R.string.no_medications_left)
                    binding.nextMedicationTime.visibility = View.GONE
                    binding.motivationCover.visibility = View.VISIBLE
                }
            }
        }



        private fun checkDateInPast(currentList: List<Medication>) {
            val today = LocalDateTime.now()
            val regularSchedList = currentList.filter {
                it.schedule is MedicationSchedule.Daily || it.schedule is MedicationSchedule.WeeklySchedule || it.schedule is MedicationSchedule.Cyclic
            }
            regularSchedList.forEach { medication ->
                val nextDueDates = when (val sched = medication.schedule) {
                    is MedicationSchedule.Daily -> sched.nextDueDates
                    is MedicationSchedule.WeeklySchedule -> sched.nextDueDates
                    is MedicationSchedule.Cyclic -> sched.nextDueDates
                    else -> emptyList()


                }
                if(nextDueDates.any { it.isBefore(today) }) {
                    Log.d(tag, "Medication ${medication.name} has a date in the past")

                }
            }
        }


        private fun getClosestDate(currentList: List<Medication>) {
            Log.d(tag, "Starting getClosestDate function")
            Log.d(tag, "Current medication list size: ${currentList.size}")

            // Filter daily and weekly schedules
            val regularSchedList = currentList.filter {
                it.schedule is MedicationSchedule.Daily || it.schedule is MedicationSchedule.WeeklySchedule || it.schedule is MedicationSchedule.Cyclic
            }
            Log.d(tag, "Filtered daily/weekly/interval schedules: ${regularSchedList.size} medications")

            val now = LocalDateTime.now()
            Log.d(tag, "Current time: $now")

            // Find the medication with the earliest upcoming or passed but not taken date
            val closestMedication = regularSchedList.minByOrNull { medication ->
                val nextDueDates = when (val sched = medication.schedule) {
                    is MedicationSchedule.Daily -> sched.nextDueDates
                    is MedicationSchedule.WeeklySchedule -> sched.nextDueDates
                    is MedicationSchedule.Cyclic -> sched.nextDueDates
                    else -> emptyList()
                }

                val closestDate = nextDueDates.filter { it.isAfter(now) || (it.isBefore(now) && !medication.medicationHistory.hasEventToday()) }
                    .minOrNull()

                Log.d(tag, "Evaluating medication: ${medication.name}, closest due date: $closestDate")
                closestDate ?: LocalDateTime.MAX
            }

            if (closestMedication != null) {
                val nextDueDates = when (val sched = closestMedication.schedule) {
                    is MedicationSchedule.Daily -> sched.nextDueDates
                    is MedicationSchedule.WeeklySchedule -> sched.nextDueDates
                    is MedicationSchedule.Cyclic -> sched.nextDueDates

                    else -> emptyList()
                }
                val closestDueDate = nextDueDates.filter { it.isAfter(now) || (it.isBefore(now) && !closestMedication.medicationHistory.hasEventToday()) }
                    .minOrNull()

                Log.d(tag, "Closest medication: ${closestMedication.name}, closest due date: $closestDueDate")

                binding.nextMedicationName.text = getString(R.string.name_of_next_med, closestMedication.name)
                if (closestDueDate != null) {
                    binding.nextMedicationTime.isVisible = true
                    binding.nextMedicationTime.text = getString(R.string.time_of_medication, closestDueDate.toLocalTime().toString())
                }
            } else {
                if(fullListOfMeds.isEmpty()) {
                    binding.nextMedicationName.text = getString(R.string.add_your_first_medication)
                    binding.nextMedicationTime.isVisible = false
                }


            }

            Log.d(tag, "Completed getClosestDate function")
        }

        private fun handleAllMedicationsCompleted() {
            Log.d(tag, "No more scheduled medications to complete!")
            binding.nextMedicationName.text = getString(R.string.no_medications_left)
            binding.nextMedicationTime.visibility = View.GONE
            binding.motivationCover.visibility = View.VISIBLE


        }
        private fun revealMotivationContent() {
            val sharedPref = requireContext().getSharedPreferences("motivation_prefs", Context.MODE_PRIVATE)
            val alreadyUncovered = sharedPref.getBoolean("already_uncovered", false)

            if (!alreadyUncovered) {
                when (val content = MotivationManager.pickOrRetrieveContentForToday(requireContext())) {
                    is MotivationManager.ContentOption.Image -> {
                        binding.specialImageView.setImageResource(content.drawableId)
                        binding.specialImageView.isVisible = true
                        binding.specialQuoteTextView.isVisible = false
                        binding.specialImageView.setOnClickListener {
                            binding.specialImageView.isVisible = false
                            sharedPref.edit().putBoolean("already_uncovered", true).apply()
                        }
                    }
                    is MotivationManager.ContentOption.Quote -> {
                        binding.specialQuoteTextView.text = content.text
                        binding.specialQuoteTextView.isVisible = true
                        binding.specialImageView.isVisible = false
                        binding.specialQuoteTextView.setOnClickListener {
                            binding.specialQuoteTextView.isVisible = false
                            sharedPref.edit().putBoolean("already_uncovered", true).apply()
                        }
                    }
                }
            }
        }

        private fun isNewDay(sharedPref: SharedPreferences): Boolean {
            val lastDate = sharedPref.getString("last_uncovered_date", null)
            val todayDate = LocalDate.now().toString()
            return lastDate == null || lastDate != todayDate
        }

        // Resets the uncover flag if it's a new day.
        private fun resetIfNewDay() {
            val sharedPref = requireContext().getSharedPreferences("motivation_prefs", Context.MODE_PRIVATE)
            if (isNewDay(sharedPref)) {
                sharedPref.edit()
                    .putBoolean("already_uncovered", false)
                    .putString("last_uncovered_date", LocalDate.now().toString())
                    .apply()
            }
        }


        /**
         * Sets up the Floating Action Button (FAB) to navigate to the AddMedicationBasicInfoFragment.
         */
        private fun setupFAB() {
            val fab = requireActivity().findViewById<ExtendedFloatingActionButton>(R.id.add_medication_fab)
            fab.setOnClickListener {
                    val navController = findNavController()
                    if(navController.currentDestination?.id == R.id.homeFragment) {
                        findNavController().navigate(R.id.action_homeFragment_to_addMedicationBasicInfoFragment)
                    }

                /*
                if(deviceConnected) {
                    val navController = findNavController()
                    if(navController.currentDestination?.id == R.id.homeFragment) {
                        findNavController().navigate(R.id.action_homeFragment_to_addMedicationBasicInfoFragment)
                    }
            } else {
                displayMessage("Device not connected to internet")
                }
                 */
                }
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
            setUpUI()
            Log.d(tag,"On Resume!")
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

        override fun onStart() {
            super.onStart()
            val filter = IntentFilter(REFRESH_ACTION)
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(refreshReceiver, filter)
        }

        override fun onStop() {
            super.onStop()
            LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(refreshReceiver)
        }
    }
