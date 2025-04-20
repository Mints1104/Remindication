package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.databinding.FragmentPrescriptionsBinding
import com.mints.mobilehealthapplication.recyclerviews.PrescriptionsRecyclerView
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel
import com.mints.mobilehealthapplication.viewmodels.HomeFragmentViewModel


/**
 * HomeFragment displays the list of medications and serves as the main screen of the application.
 * It uses a ViewModel to fetch and observe medication data and manages the UI using ViewBinding.
 */
class PrescriptionsFragment : Fragment() {

    private var _binding: FragmentPrescriptionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PrescriptionsRecyclerView
    private var uid = ""
    private var medicationList = MutableLiveData<List<Medication>>()
    private lateinit var notificationHelper: NotificationHelper
    private var tag = "PrescriptionsFrag"
    private var deviceConnected = false
    private var meds = mutableListOf<Medication>()

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
        _binding = FragmentPrescriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Initializes UI components, sets up RecyclerView, and fetches medication data after the view is created.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationHelper = NotificationHelper(requireContext())

        setUpRecyclerView()
        fetchUserMedication()
        viewModel.getCurrentDay()
        deviceConnected = isDeviceConnected()
        observeNetworkState()

        setUpUI()


    }

    private fun setUpUI() {
        mainActivity.showBottomNav()
    }

    private fun observeNetworkState() {
        mainActivity.internetChecker.connectionState.observe(viewLifecycleOwner) { isConnected ->
            deviceConnected = isConnected
        }
    }

    private fun isDeviceConnected(): Boolean {
        return mainActivity.checkNetworkState()
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

           if(medicationList.value?.isEmpty() == true) {
               Log.d("Test","User has no medications added.")
               binding.noMedicationsAdded.isVisible = true
           }

        }
    }

    private fun handleDisplayingNotes(medication: Medication) {
        val addMedicationNotesBottomSheet =
            MedicationNotesBottomSheet.newInstance(medication.name, medication.notes)
        addMedicationNotesBottomSheet.show(parentFragmentManager, "MedicationNotesBottomSheet")
    }


    /**
     * Sets up the RecyclerView to display a list of medications.
     * Observes LiveData from the ViewModel to update the medication list dynamically.
     */
    private fun setUpRecyclerView() {
        Log.d("HomeFragment", "Setting up RecyclerView")

        adapter = PrescriptionsRecyclerView(emptyList()) { medication ->
            viewModel.onMedicationClicked(medication)
            when (val schedule = medication.schedule) {
                is MedicationSchedule.Daily -> {

                    Log.d("MED_TEST", "Times: ${schedule.times}")
                    Log.d("MED_TEST", "Calculated Dates: ${schedule.nextDueDates}")

                    viewModel.testReceivingMedicationHistory(medication)

                }

                is MedicationSchedule.WeeklySchedule -> {
                    Log.d("MED_TEST", "Times: ${schedule.times}")
                    Log.d("MED_TEST", "Calculated Dates: ${schedule.nextDueDates}")
                    viewModel.testReceivingMedicationHistory(medication)

                }
                is MedicationSchedule.Cyclic -> {
                    Log.d("CyclicTest","Cyclic: Times: ${schedule.times}")
                    Log.d("CyclicTest","Cyclic: next due dates: ${schedule.nextDueDates}")

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
            meds = medications.toMutableList()
            meds.sortBy { it.name.lowercase() }
            adapter.updateMedicationList(meds)
        }

        Log.d(tag, "RecyclerView setup complete")
    }





    private fun addSwipeFunctionality() {
        val swipeCallback = MaterialSwipeCallback(
            context = requireContext(),
            swipeLeftAction = MaterialSwipeCallback.SwipeAction(
                iconRes = R.drawable.baseline_delete_24px,
                backgroundColorRes = android.R.color.holo_red_dark,
                label = "Delete Medication"
            ),
            swipeRightAction = MaterialSwipeCallback.SwipeAction(
                iconRes = R.drawable.baseline_edit_24px,
                backgroundColorRes = R.color.material_yellow,
                label = "Edit Medication"
            ),
            onSwipeLeft = { position, onActionCompleted ->
                val medication = adapter.getMedicationAt(position)
//                if (deviceConnected) {
//                    displayMessage("Delete medication: ${medication.name}")
//                    showUndoSnackbar(medication, position, onActionCompleted)
//                } else {
//                    displayMessage("Device not connected to internet")
//                    onActionCompleted()
//                }
                                    showUndoSnackbar(medication, position, onActionCompleted)

            },
            onSwipeRight = { position, onActionCompleted ->
                val medication = adapter.getMedicationAt(position)
              //  if (deviceConnected) {
                    val medicationId = medication.id
                    val action = PrescriptionsFragmentDirections
                        .actionPrescriptionsFragmentToAddMedicationBasicInfoFragment(medicationId!!)
                    val navController = findNavController()
                    if (navController.currentDestination?.id == R.id.prescriptionsFragment) {
                        navController.navigate(action)
                    }
                    onActionCompleted()
               // } else {
               //     displayMessage("Device not connected to internet")
               //     onActionCompleted()
               // }
            }
        )
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.medicationsRecyclerView)
    }



    private fun showUndoSnackbar(medication: Medication, position: Int, onActionCompleted: () -> Unit) {
        // Get the current list and remove the medication
        val oldList = adapter.getMedicationList().toMutableList()
        val removedIndex = oldList.indexOf(medication)
        if (removedIndex == -1) {
            Log.e("SwipeDebug", "Medication ${medication.name} not found in list!")
            onActionCompleted()
            return
        }
        oldList.removeAt(removedIndex)
        adapter.updateMedicationList(oldList) // This uses DiffUtil to remove it

        Log.d("SwipeDebug", "After remove: $oldList, size: ${oldList.size}")

        Snackbar.make(binding.root, "${medication.name} deleted", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                val currentList = adapter.getMedicationList().toMutableList()
                currentList.add(removedIndex, medication)
                adapter.updateMedicationList(currentList) // DiffUtil adds it back
                Log.d("SwipeDebug", "After undo: $currentList, size: ${currentList.size}")
                onActionCompleted()
            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) {
                        medication.id?.let { id ->
                            viewModel.deleteMedication(uid, id) {
                                Log.d("SwipeDebug", "Deleted ${medication.name} from backend")
                                onActionCompleted()
                            }
                        } ?: onActionCompleted()
                    } else {
                        onActionCompleted()
                    }
                    onActionCompleted()

                }

            })
            .show()
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
    }



    /**
     * Cleans up ViewBinding to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        val viewModel: AddMedicationViewModel by activityViewModels()
        Log.d("PrescriptionsFrag","Resetting all data.")
        viewModel.resetValidationState()
        _binding = null
    }
}
