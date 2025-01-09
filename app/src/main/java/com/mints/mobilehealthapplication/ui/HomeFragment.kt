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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R

class HomeFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_loginscreen, container, false)






        return rootView
    }



    override fun onResume() {
        super.onResume()
        Log.d("LoginFragment","In Onresume...")
    }

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }
}
