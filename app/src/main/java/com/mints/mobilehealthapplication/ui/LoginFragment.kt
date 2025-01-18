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
    ): View? {
        _binding = FragmentLoginscreenBinding.inflate(inflater, container, false)

        val mainActivity = requireActivity() as MainActivity
        mainActivity.hideAllUI()

        Log.d("LoginFragment", "This is the login fragment.")

        val signUpText: TextView = binding.newUserText
        val forgotPasswordText: TextView = binding.forgotUserPasswordText
        val loginButton: Button = binding.loginButton

        forgotPasswordText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_resetPasswordFragment)
        }

        loginButton.setOnClickListener { view ->
            loginClick(view)
        }

        signUpText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_userInfoFragment)
        }

        return binding.root
    }

    /**
     * Handles the login button click event.
     *
     * @param view The view that was clicked.
     */
    private fun loginClick(view: View) {
        val emailText = binding.emailEditText.text.toString().trim()
        val passwordText = binding.passwordEditText.text.toString().trim()

        // Call the ViewModel to handle login logic
        viewModel.login(emailText, passwordText) { success, message ->
            if (success) {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            } else {
                // Show error message if login fails
                displayMessage(view, message)
            }
        }
    }

    /**
     * Lifecycle callback invoked when the fragment resumes.
     */
    override fun onResume() {
        super.onResume()
        Log.d("LoginFragment", "In OnResume...")
    }

    /**
     * Displays a message to the user using a Snackbar.
     *
     * @param view The parent view to attach the Snackbar to.
     * @param msgTxt The message text to display.
     */
    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Lifecycle callback invoked when the fragment's view is destroyed.
     * Clears references to avoid memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("LoginFragment", "View destroyed, cleaning up resources.")
    }
}
