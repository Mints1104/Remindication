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
import com.mints.mobilehealthapplication.databinding.FragmentResetPasswordScreenBinding
import com.mints.mobilehealthapplication.viewmodels.ResetPasswordViewModel


class ResetPasswordFragment : Fragment() {

    private val viewModel: ResetPasswordViewModel by viewModels()
    private var _binding: FragmentResetPasswordScreenBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordScreenBinding.inflate(inflater, container, false)

        val emailInput: EditText = binding.emailEditText
        val resetButton: Button = binding.resetPasswordButton
        val goBackButton: Button = binding.goBackButton

        resetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()

            viewModel.sendPasswordResetEmail(email) { _, message ->
                displayMessage(message)
            }
        }

        goBackButton.setOnClickListener {
            findNavController().navigate(R.id.action_resetPasswordFragment_to_loginFragment)
        }

        return binding.root
    }


    private fun displayMessage(msgTxt: String) {
        Snackbar.make(binding.root, msgTxt, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
