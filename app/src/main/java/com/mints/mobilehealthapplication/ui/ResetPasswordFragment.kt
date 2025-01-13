package com.mints.mobilehealthapplication.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.viewmodels.ResetPasswordViewModel

class ResetPasswordFragment : Fragment() {

    private val viewModel: ResetPasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_reset_password_screen, container, false)

        val emailInput: EditText = rootView.findViewById(R.id.email_edit_text)
        val resetButton: Button = rootView.findViewById(R.id.reset_password_button)
        val goBackButton: Button = rootView.findViewById(R.id.go_back_button)

        resetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            viewModel.sendPasswordResetEmail(email) { _, message ->
                displayMessage(requireView(), message)
            }
        }

        goBackButton.setOnClickListener {
            findNavController().navigate(R.id.action_resetPasswordFragment_to_loginFragment)
        }

        return rootView
    }

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }
}