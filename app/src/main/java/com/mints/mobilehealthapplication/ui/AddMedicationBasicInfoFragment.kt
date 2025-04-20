package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.FragmentAddMedicationPart1Binding
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel
import kotlinx.coroutines.launch

/**
 * A Fragment to handle adding a medication entry.
 * Allows the user to input medication details such as name, dosage, frequency, reminder time, and notes.
 */
class AddMedicationBasicInfoFragment : Fragment() {

    private var _binding: FragmentAddMedicationPart1Binding? = null
    private val binding get() = _binding!!
    private var tag = "BasicInfoFragment"
    private var uid = ""
    private val viewModel: AddMedicationViewModel by activityViewModels()
    private var deviceConnected = false
    private val mainActivity: MainActivity by lazy {
        requireActivity() as MainActivity
    }

    /**
     * Inflates the layout and initializes UI elements for the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMedicationPart1Binding.inflate(inflater, container, false)
        val view = binding.root
        viewModel.resetValidationState()
        viewModel.testDateLogic()
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        setUpUI()
        setUpTextInputListeners()
        setupContinueButton()
        observeValidationState()
        Log.d(tag,"UserId: $uid")
        deviceConnected = isDeviceConnected()
        /*
        observeNetworkState()

        if(!deviceConnected) {
            displayMessage("Internet connection lost")
            findNavController().navigate(R.id.action_addMedicationBasicInfoFragment_to_homeFragment)
        } else {
            Log.d(tag, "Device is connected to the internet")
        }

         */

        return view
    }


    private fun setUpUI() {
        mainActivity.apply {
            hideFAB()
            hideBottomNav()
        }
        checkIfEditingMedication(mainActivity)
    }

    private fun revertUI() {
        mainActivity.showBottomNav()
    }


    private fun checkIfEditingMedication(mainActivity: MainActivity) {
        val args: AddMedicationBasicInfoFragmentArgs by navArgs()

        val medicationId = args.medicationId
        if (medicationId.isNotEmpty()) {
            viewModel.setIsEditing(true)
            mainActivity.updateToolBarTitle("Edit Medication")
            lifecycleScope.launch {
                viewModel.getMedicationDetails(uid, medicationId)
                Log.d(tag, "Name: ${viewModel.getName()}")
                Log.d(tag, "Dosage: ${viewModel.getDosage()}")
                Log.d(tag, "Notes: ${viewModel.getNotes()}")
                binding.medicationNameEditText.setText(viewModel.getName())
                binding.dosageEditText.setText(viewModel.getDosage())
                binding.notesEditText.setText(viewModel.getNotes())

            }
        }
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

    private fun setUpTextInputListeners() {
        binding.medicationNameEditText.doAfterTextChanged { text
            ->
            Log.d(tag,"Medication name inputted: $text")
            viewModel.updateMedicationName(text?.toString() ?: "")
        }
        binding.dosageEditText.doAfterTextChanged { text ->
            Log.d(tag,"Dosage  inputted: $text")
            viewModel.updateDosage(text?.toString() ?: "")
        }
        binding.notesEditText.doAfterTextChanged { text ->
            Log.d(tag,"Notes inputted: $text")
            viewModel.updateNotes(text?.toString() ?: "")
        }
    }


    private fun setupContinueButton() {
        binding.continueMedicationButton.setOnClickListener {
            Log.d("AddMedicationViewModel", "Button clicked")
            Log.d("AddMedicationViewModel", "Current EditText value: ${binding.medicationNameEditText.text}")
            Log.d("AddMedicationViewModel", "Pre-validation name value: ${viewModel.getName()}")
            viewModel.validateBasicInfo()
        }
    }


    private fun navigateToNextFragment() {
        if (isCurrentDestinationValid()) {
            findNavController().navigate(R.id.action_addMedicationBasicInfoFragment_to_addMedicationFrequencyFragment)
            viewModel.resetValidationState()
        }
    }


    private fun isCurrentDestinationValid(): Boolean {
        return findNavController().currentDestination?.id == R.id.addMedicationBasicInfoFragment
    }

    private fun observeNetworkState() {
        mainActivity.internetChecker.connectionState.observe(viewLifecycleOwner) { isConnected ->
            if (!isConnected) {
                displayMessage("Internet connection lost")
                if (findNavController().currentDestination?.id == R.id.addMedicationBasicInfoFragment) {
                    findNavController().navigate(R.id.action_addMedicationBasicInfoFragment_to_homeFragment)
                }
            }
        }
    }

    private fun isDeviceConnected(): Boolean {
        return mainActivity.checkNetworkState()
    }


    override fun onPause() {
        super.onPause()
        Log.d(tag, "onPause called, current name: ${viewModel.getName()}")
    }


    override fun onResume() {
        super.onResume()
        setUpUI()
        Log.d(tag, "onResume called, current name: ${viewModel.getName()}")
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
        viewModel.validationState.removeObservers(viewLifecycleOwner)
        super.onDestroyView()
        Log.d("AddMedicationBasicInfoFragment", "onDestroyView called")
        _binding = null
    }
}
