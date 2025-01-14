package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.viewmodels.LoginViewModel

class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var email: EditText
    private lateinit var password: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_loginscreen, container, false)

        // Hide UI elements
        (requireActivity() as MainActivity).hideFAB()
        val mainActivity = requireActivity() as MainActivity
        mainActivity.hideAppBar()
        mainActivity.hideBottomNav()

        Log.d("LoginFragment", "This is the login fragment.")

        // Initialize views
        email = rootView.findViewById(R.id.email_edit_text)
        password = rootView.findViewById(R.id.password_edit_text)
        val signUpText: TextView = rootView.findViewById(R.id.new_user_text)
        val forgotPasswordText: TextView = rootView.findViewById(R.id.forgot_user_password_text)
        val loginButton: Button = rootView.findViewById(R.id.login_button)

        // Set up click listeners
        forgotPasswordText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_resetPasswordFragment)
        }

        loginButton.setOnClickListener { view -> loginClick(view) }

        signUpText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_userInfoFragment)
        }

        return rootView
    }

    private fun loginClick(view: View) {
        val emailText = email.text.toString().trim()
        val passwordText = password.text.toString().trim()

        // Call the ViewModel to handle login
        viewModel.login(emailText, passwordText) { success, message ->
            if (success) {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            } else {
                // Show error message
                displayMessage(view, message)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("LoginFragment", "In OnResume...")
    }

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }
}