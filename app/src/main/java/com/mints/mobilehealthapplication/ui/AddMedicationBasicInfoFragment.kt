package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.databinding.FragmentAddMedicationPart1Binding
import com.mints.mobilehealthapplication.viewmodels.AddMedicationViewModel

/**
 * A Fragment to handle adding a medication entry.
 * Allows the user to input medication details such as name, dosage, frequency, reminder time, and notes.
 */
class AddMedicationBasicInfoFragment : Fragment() {

    private var _binding: FragmentAddMedicationPart1Binding? = null
    private val binding get() = _binding!!
    private var tag = "AddMedicationBasicInfoFragment"

    private lateinit var viewModel: AddMedicationViewModel


    /**
     * Inflates the layout and initializes UI elements for the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using ViewBinding
        _binding = FragmentAddMedicationPart1Binding.inflate(inflater, container, false)
        val view = binding.root



        val mainActivity = activity as MainActivity
        mainActivity.hideFAB()
        mainActivity.hideBottomNav()

        viewModel = ViewModelProvider(this)[AddMedicationViewModel::class.java]



        setUpTextInputListeners()
        confirmInputAndContinue()




        return view
    }

    private fun setUpTextInputListeners() {
        binding.medicationNameEditText.doAfterTextChanged { text
            ->
            Log.d(tag,"Medication name inputted: $text")
            viewModel.updateMedicationName(text.toString())

        }




        binding.dosageEditText.doAfterTextChanged { text ->
            Log.d(tag,"Dosage  inputted: $text")

            viewModel.updateDosage(text.toString())
        }


        binding.notesEditText.doAfterTextChanged { text ->
            Log.d(tag,"Notes inputted: $text")

            viewModel.updateNotes(text.toString())
        }
    }

    private fun confirmInputAndContinue() {
        binding.continueMedicationButton.setOnClickListener {
            Log.d(tag,"Continue button clicked")

            if(viewModel.validateInputs()) {

                val stage1Data = viewModel.getStage1Data()
                displayMessage(requireView(),"Input is good!")

            } else {
                displayMessage(requireView(),"Some inputs are missing!")

            }
        }



    }




    /**
     * Displays a message in a Snackbar at the bottom of the screen.
     */
    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
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
