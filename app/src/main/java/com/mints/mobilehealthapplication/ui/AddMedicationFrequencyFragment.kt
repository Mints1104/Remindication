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
    private var tag = "AddMedicationFrequencyFragment"

    private val viewModel: AddMedicationViewModel by activityViewModels()

    /**
     * Inflates the layout and initializes UI elements for the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using ViewBinding
        _binding = FragmentAddMedicationPart2Binding.inflate(inflater, container, false)
        val view = binding.root
        viewModel.resetValidationState()






        val mainActivity = activity as MainActivity
        mainActivity.hideFAB()
        mainActivity.hideBottomNav()




        setUpRadioButtonListeners()
        setupContinueButton()
        observeValidationState()



        return view
    }


    private fun observeValidationState() {
        viewModel.validationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AddMedicationViewModel.ValidationState.Invalid -> {
                    displayMessage(state.message)
                    viewModel.resetValidationState()
                }
                AddMedicationViewModel.ValidationState.Valid -> {
                    navigateToNextFragment()
                    viewModel.resetValidationState()

                }
                AddMedicationViewModel.ValidationState.Initial -> {}
            }
        }
    }


    private fun setUpRadioButtonListeners() {
        // Restore previous selection FIRST
        viewModel.frequency.value?.let { freq ->
            val radioId = when (freq) {
                "Once Daily" -> R.id.onceDailyButton
                "Twice Daily" -> R.id.twiceDailyButton
                "Weekly" -> R.id.weeklyButton
                "Cyclic" -> R.id.cyclicButton
                "On Demand" -> R.id.onDemandButton
                else -> -1
            }
            if (radioId != -1) binding.radioGroup.check(radioId)
        }

        // Then set up listener
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val frequency = when(checkedId) {
                R.id.onceDailyButton -> "Once Daily"
                R.id.twiceDailyButton -> "Twice Daily"
                R.id.weeklyButton -> "Weekly"
                R.id.cyclicButton -> "Cyclic"
                R.id.onDemandButton -> "On Demand"
                else -> null
            }
            frequency?.let { viewModel.updateFrequency(it) }
        }
    }



    private fun setupContinueButton() {
        binding.continueMedicationButton.setOnClickListener {
            viewModel.validateFrequency()
        }
    }

    private fun navigateToNextFragment() {
        if (isCurrentDestinationValid()) {
         //   findNavController().navigate(R.id.action_addMedicationBasicInfoFragment_to_addMedicationFrequencyFragment)
            viewModel.resetValidationState()
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
        Log.d("AddMedicationBasicInfoFragment", "onDestroyView called")
        _binding = null
    }
}
