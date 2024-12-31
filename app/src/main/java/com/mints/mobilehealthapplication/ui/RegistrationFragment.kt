package com.mints.mobilehealthapplication.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R

class RegistrationFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_registrationscreen, container, false)

        auth = Firebase.auth

        email = rootView.findViewById(R.id.email_edit_text)
        username = rootView.findViewById(R.id.username_edit_text)
        password = rootView.findViewById(R.id.password_edit_text)
        confirmPassword = rootView.findViewById(R.id.confirm_password_edit_text)

        val signUpButton: Button = rootView.findViewById(R.id.signup_button)
        signUpButton.setOnClickListener { view ->
            registerUser(view)
        }

        val alreadyHaveAccount: TextView = rootView.findViewById(R.id.already_has_an_account)
        alreadyHaveAccount.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        return rootView
    }

    private fun registerUser(view: View) {
        Log.i("Registration", "Sign-up clicked")

        val emailText = email.text.toString().trim()
        val usernameText = username.text.toString().trim()
        val passwordText = password.text.toString().trim()
        val confirmPasswordText = confirmPassword.text.toString().trim()

        if (emailText.isEmpty() || usernameText.isEmpty() || passwordText.isEmpty() || confirmPasswordText.isEmpty()) {
            displayMessage(view, getString(R.string.all_fields_are_required))
            return
        }

        if (passwordText != confirmPasswordText) {
            displayMessage(view, getString(R.string.passwords_do_not_match))
            return
        }

        auth.createUserWithEmailAndPassword(emailText, passwordText)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    closeKeyboard()
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(usernameText)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            displayMessage(view, getString(R.string.registration_successful))
                            Log.i("Registration", "User registered with username: $usernameText")


                            val intent = Intent(requireActivity(), MainActivity::class.java)
                            intent.putExtra("new_user", true)
                            startActivity(intent)



                        }
                    }
                } else {
                    displayMessage(view, "Error: ${task.exception?.message}")
                    Log.e("Registration", "Error: ${task.exception?.message}")
                }
            }
    }

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

    private fun closeKeyboard() {
        val view = activity?.currentFocus
        if (view != null) {
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
