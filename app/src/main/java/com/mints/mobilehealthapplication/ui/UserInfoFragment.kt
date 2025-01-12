package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel

class UserInfoFragment : Fragment() {

    private lateinit var viewModel: RegistrationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration, container, false)

        val mainActivity = requireActivity() as MainActivity
        mainActivity.hideAppBarAndBottomNav()

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]

        // Bind UI elements
        val emailEditText = view.findViewById<EditText>(R.id.email_edit_text)
        val phoneEditText = view.findViewById<EditText>(R.id.phone_edit_text)
        val passwordEditText = view.findViewById<EditText>(R.id.password_edit_text)
        val confirmPasswordEditText = view.findViewById<EditText>(R.id.confirm_password_edit_text)

        // Save data to ViewModel
        val continueButton = view.findViewById<Button>(R.id.continue_button)
        continueButton.setOnClickListener {
            // Get input values
            val email = emailEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Validate inputs
            if (email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update ViewModel
            viewModel.updateRegistrationData {
                this.email = email
                this.phoneNumber = phone
                this.password = password
            }

            // Navigate to the next fragment
            findNavController().navigate(R.id.action_userInfoFragment_to_healthInfoFragment)
        }

        return view
    }

    // Function to validate email format
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}