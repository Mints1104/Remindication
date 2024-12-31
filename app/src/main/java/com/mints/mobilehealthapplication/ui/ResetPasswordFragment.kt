package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R


class ResetPasswordFragment : Fragment() {

    private lateinit var auth: FirebaseAuth


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

            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            displayMessage(requireView(),
                                getString(R.string.password_reset_email_sent))
                        } else {
                            val errorMessage = task.exception?.message ?: "Error occurred"
                            displayMessage(requireView(), errorMessage)

                        }
                    }
            } else {
                displayMessage(requireView(), getString(R.string.please_enter_a_valid_email_address))
            }
        }

        goBackButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        auth = Firebase.auth

        return rootView
    }

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

}
