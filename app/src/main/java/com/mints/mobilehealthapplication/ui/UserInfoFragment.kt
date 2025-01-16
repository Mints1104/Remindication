package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel

/**
 * Fragment responsible for handling the user information screen during the registration process.
 * Validates and collects user email, phone, and password inputs.
 */
class UserInfoFragment : Fragment() {

    private lateinit var viewModel: RegistrationViewModel

    /**
     * Called to inflate the fragment's view and initialize the necessary components.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration, container, false)

        // Hide the app bar and bottom navigation on this fragment's view
        val mainActivity = requireActivity() as MainActivity
        mainActivity.showAppBar()

        // Initialize ViewModel for managing the registration data
        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]

        // Bind UI elements to local variables
        val emailEditText = view.findViewById<EditText>(R.id.email_edit_text)
        val phoneEditText = view.findViewById<EditText>(R.id.phone_edit_text)
        val passwordEditText = view.findViewById<EditText>(R.id.password_edit_text)
        val confirmPasswordEditText = view.findViewById<EditText>(R.id.confirm_password_edit_text)
        val alreadyHaveAccountTextView = view.findViewById<TextView>(R.id.already_have_account_text)
        emailEditText.setText(viewModel.registrationData.value.email)
        phoneEditText.setText(viewModel.registrationData.value.phoneNumber)
        passwordEditText.setText(viewModel.registrationData.value.password)
        confirmPasswordEditText.setText(viewModel.registrationData.value.password)

        alreadyHaveAccountTextView.setOnClickListener {
            findNavController().navigate(R.id.action_userInfoFragment_to_loginFragment)

        }

        // Set up listener for the continue button
        val continueButton = view.findViewById<Button>(R.id.continue_button)
        continueButton.setOnClickListener {
            // Get the user input values from the EditText fields
            val email = emailEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Validate the input fields
            if (email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                // Show a message if any field is empty
                displayMessage(requireView(),getString(R.string.fill_in_all_fields))
                return@setOnClickListener
            }

            // Validate email format using a regex pattern
            if (!isValidEmail(email)) {
                displayMessage(requireView(),getString(R.string.please_enter_a_valid_email_address))
                return@setOnClickListener
            }

            // Ensure that the password and confirmation match
            if (password != confirmPassword) {
                displayMessage(requireView(),getString(R.string.passwords_do_not_match))

                return@setOnClickListener
            }

            // Update the ViewModel with the collected registration data
            viewModel.updateRegistrationData {
                this.email = email
                this.phoneNumber = phone
                this.password = password
            }

            // Navigate to the next fragment (health information screen)
            findNavController().navigate(R.id.action_userInfoFragment_to_healthInfoFragment)
        }

        return view
    }

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Helper function to validate the email format using a regex pattern.
     *
     * @param email The email address to be validated
     * @return true if the email format is valid, false otherwise
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}