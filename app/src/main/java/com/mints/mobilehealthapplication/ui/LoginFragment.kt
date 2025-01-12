package com.mints.mobilehealthapplication.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_loginscreen, container, false)
        (requireActivity() as MainActivity).hideFAB()
        val signUpText: TextView = rootView.findViewById(R.id.new_user_text)
        val forgotPasswordText: TextView = rootView.findViewById(R.id.forgot_user_password_text)
        val loginButton: Button = rootView.findViewById(R.id.login_button)
        val mainActivity = requireActivity() as MainActivity
        mainActivity.hideAppBarAndBottomNav()
        Log.d("LoginFragment","This is the login fragment.")
        email = rootView.findViewById(R.id.email_edit_text)
        password = rootView.findViewById(R.id.password_edit_text)

        auth = Firebase.auth

        forgotPasswordText.setOnClickListener {


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

        if (emailText.isEmpty() || passwordText.isEmpty()) {
            displayMessage(view, getString(R.string.please_enter_both_email_and_password))
            return
        }

        auth.signInWithEmailAndPassword(emailText, passwordText)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(activity, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    displayMessage(view, getString(R.string.login_failed, task.exception?.message))
                }
            }
    }

    override fun onResume() {
        super.onResume()
        Log.d("LoginFragment","In Onresume...")
    }

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }
}
