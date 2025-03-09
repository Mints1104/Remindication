package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.FragmentAddMedicationPart2Binding
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel

/**
 * A Fragment to handle adding a medication entry.
 * Allows the user to input medication details such as name, dosage, frequency, reminder time, and notes.
 */
class AddMedicationFrequencyFragment : Fragment() {

    private var _binding: FragmentAddMedicationPart2Binding? = null
    private val binding get() = _binding!!
    private var tag = "M.FrequencyFragment"
    private val viewModel: AddMedicationViewModel by activityViewModels()


    /**
     * Inflates the layout and initializes UI elements for the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(tag, "onCreateView: Inflating layout")
        _binding = FragmentAddMedicationPart2Binding.inflate(inflater, container, false)
        val view = binding.root
        viewModel.resetValidationState()
        setUpUI()
        setUpRadioButtonListeners()
        setupContinueButton()
        observeValidationState()
        return view
    }

    private fun setUpUI() {
        val mainActivity = activity as MainActivity
        mainActivity.hideFAB()
        mainActivity.hideBottomNav()
        setUpForEditing(mainActivity)
    }


    private fun setUpForEditing(mainActivity: MainActivity) {
        val isEditing = viewModel.getIsEditing()
        if(isEditing == true) {
            mainActivity.updateToolBarTitle("Edit Medication")
            Log.d(tag,"Test getting frequency: ${viewModel.getFrequency()}")
            Log.d(tag,"Test getting frequency type: ${viewModel.getFrequencyType()}")
        }
    }


    private fun observeValidationState() {
        Log.d(tag, "observeValidationState: Observing validation state")
        viewModel.validationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AddMedicationViewModel.ValidationState.Invalid -> {
                    Log.d(tag, "observeValidationState: Invalid state - ${state.message}")
                    displayMessage(state.message)
                    viewModel.resetValidationState()
                }
                AddMedicationViewModel.ValidationState.Valid -> {
                    Log.d(tag, "observeValidationState: Valid state - Navigating to next fragment")
                    navigateToNextFragment()
                    viewModel.resetValidationState()
                }
                AddMedicationViewModel.ValidationState.Initial -> {
                    Log.d(tag, "observeValidationState: Initial state")
                }
            }
        }
    }


    private fun setUpRadioButtonListeners() {
        Log.d(tag, "setUpRadioButtonListeners: Restoring previous selection")
        Log.d(tag,"Test before restoring prev selection: ${viewModel.frequency.value}")
        Log.d(tag,"Test before restoring prev selection: ${viewModel.getFrequencyType()}")
        if(viewModel.getIsEditing() == true) {
            Log.d(tag,"We are editing so use saved frequency type: ${viewModel.getFrequencyType()}")
            viewModel.frequencyType.value?.let { freq ->
               val radioId = when(freq) {
                   "Once Daily" -> R.id.onceDailyButton
                   "Twice Daily" -> R.id.twiceDailyButton
                   "Weekly" -> R.id.weeklyButton
                   "Cyclic" -> R.id.cyclicButton
                   "On demand" -> R.id.onDemandButton
                   else -> -1
               }
                if (radioId != -1) binding.radioGroup.check(radioId)
            }
        } else {
            Log.d(tag,"We are not editing")
            viewModel.frequency.value?.let { freq ->
                val radioId = when (freq) {
                    "Once Daily" -> R.id.onceDailyButton
                    "Twice Daily" -> R.id.twiceDailyButton
                    "Weekly" -> R.id.weeklyButton
                    "Cyclic" -> R.id.cyclicButton
                    "On Demand", "As Needed" -> R.id.onDemandButton
                    else -> -1
                }
                if (radioId != -1) binding.radioGroup.check(radioId)
                Log.d(tag, "setUpRadioButtonListeners: Previous selection restored - $freq")
            }
        }
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val frequency = when(checkedId) {
                R.id.onceDailyButton -> "Once Daily"
                R.id.twiceDailyButton -> "Twice Daily"
                R.id.weeklyButton -> "Weekly"
                R.id.cyclicButton -> "Cyclic"
                R.id.onDemandButton -> "On Demand"
                else -> null
            }
            frequency?.let {
                Log.d(tag, "setUpRadioButtonListeners: Selected frequency - $it")
                viewModel.updateFrequency(it)
                viewModel.updateFrequencyType(it)

            }
        }
    }


    private fun setupContinueButton() {
        binding.continueMedicationButton.setOnClickListener {
            Log.d(tag, "setupContinueButton: Continue button clicked")
            viewModel.validateFrequency()
        }
    }


    private fun navigateToNextFragment() {
        if (isCurrentDestinationValid()) {
            Log.d(tag, "navigateToNextFragment: Navigating to schedule fragment")
            findNavController().navigate(R.id.action_addMedicationFrequencyFragment_to_addMedicationScheduleFragment)
            viewModel.resetValidationState()
        } else {
            Log.d(tag, "navigateToNextFragment: Navigation aborted, invalid destination")
        }
    }


    private fun isCurrentDestinationValid(): Boolean {
        return findNavController().currentDestination?.id == R.id.addMedicationFrequencyFragment
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
     * Called when the view is destroyed, cleans up resources.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(tag, "onDestroyView: Cleaning up resources")
        _binding = null
    }
}
