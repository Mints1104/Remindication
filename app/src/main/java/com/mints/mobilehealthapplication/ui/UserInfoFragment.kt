package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.FragmentRegistrationBinding
import com.mints.mobilehealthapplication.viewmodels.RegistrationViewModel

/**
 * Fragment responsible for handling the user information screen during the registration process.
 * Validates and collects user email, phone, and password inputs.
 */
class UserInfoFragment : Fragment() {
    private lateinit var viewModel: RegistrationViewModel
    private var _binding: FragmentRegistrationBinding? = null
    private val binding get() = _binding!!
    private val mainActivity: MainActivity by lazy {
        requireActivity() as MainActivity
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]
        viewModel.resetRegistrationData()

        mainActivity.showAppBar()
        setUpUI()
        handleContinueRegistration()
        return binding.root
    }



    private fun handleContinueRegistration() {
        binding.continueButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val phone = binding.phoneEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                displayMessage(getString(R.string.fill_in_all_fields))
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                displayMessage(getString(R.string.please_enter_a_valid_email_address))
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                displayMessage(getString(R.string.passwords_do_not_match))
                return@setOnClickListener
            }

            viewModel.updateRegistrationData {
                this.email = email
                this.phoneNumber = phone
                this.password = password
            }

            val navController = findNavController()
            if(navController.currentDestination?.id == R.id.userInfoFragment) {
                navController.navigate(R.id.action_userInfoFragment_to_healthInfoFragment)
            }
        }
    }

    private fun setUpUI() {
        binding.emailEditText.setText(viewModel.registrationData.value.email)
        binding.phoneEditText.setText(viewModel.registrationData.value.phoneNumber)
        binding.passwordEditText.setText(viewModel.registrationData.value.password)
        binding.confirmPasswordEditText.setText(viewModel.registrationData.value.password)
        binding.alreadyHaveAccountText.setOnClickListener {
            findNavController().navigate(R.id.action_userInfoFragment_to_loginFragment)
        }
    }


    private fun displayMessage(msgTxt: String) {
        Snackbar.make(binding.root, msgTxt, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
