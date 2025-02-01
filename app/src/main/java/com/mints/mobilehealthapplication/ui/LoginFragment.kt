package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.FragmentLoginscreenBinding
import com.mints.mobilehealthapplication.viewmodels.LoginViewModel

/**
 * LoginFragment handles the user login functionality, including user authentication
 * and navigation to related fragments like "Reset Password" or "Sign Up."
 */
class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()
    private var _binding: FragmentLoginscreenBinding? = null
    private val binding get() = _binding!!

    /**
     * Inflates the layout for this fragment and initializes the UI components.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginscreenBinding.inflate(inflater, container, false)
        Log.d("LoginFragment", "This is the login fragment.")
        setUpUI()

        return binding.root
    }


    private fun setUpUI() {
        val mainActivity = requireActivity() as MainActivity
        mainActivity.hideAllUI()
        val signUpText: TextView = binding.newUserText
        val forgotPasswordText: TextView = binding.forgotUserPasswordText
        val loginButton: Button = binding.loginButton
        forgotPasswordText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_resetPasswordFragment)
        }
        loginButton.setOnClickListener {
            loginClick()
        }
        signUpText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_userInfoFragment)
        }
    }


    private fun loginClick() {
        val emailText = binding.emailEditText.text.toString().trim()
        val passwordText = binding.passwordEditText.text.toString().trim()
        viewModel.login(emailText, passwordText) { success, message ->
            if (success) {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            } else {
                displayMessage(message)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("LoginFragment", "In OnResume...")
    }


    private fun displayMessage(msgTxt: String) {
        Snackbar.make(binding.root, msgTxt, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("LoginFragment", "View destroyed, cleaning up resources.")
    }
}
