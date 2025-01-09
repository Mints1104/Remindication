package com.mints.mobilehealthapplication.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R

class MainActivity : AppCompatActivity() {
    private lateinit var mToolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var db: FirebaseFirestore

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        mToolbar = findViewById(R.id.main_toolbar)
        if (currentUser != null) {
            loadFragment(HomeFragment())
            showAppBarAndBottomNav()
            val userId = auth.currentUser!!.uid

            retrieveUserInfo(userId)

        } else {
            loadFragment(LoginFragment())
            hideAppBarAndBottomNav()

        }
    }

    private fun retrieveUserInfo(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    Log.d("Firestore", "Document ID: ${document.id}, Data: ${document.data}")
                } else {
                    Log.d("Firestore", "No such document")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error reading document", e)
            }
    }

    private fun loadFragment(fragment: Fragment) {
        if (!isFinishing) {
            val transaction = supportFragmentManager.beginTransaction()

            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment != fragment) {
                transaction.replace(R.id.fragment_container, fragment)
                transaction.addToBackStack(fragment.tag)
                transaction.commit()
            }
        }
    }




    private fun hideAppBarAndBottomNav() {
        mToolbar.isVisible = false
        bottomNavigation.isVisible = false
    }

    private fun showAppBarAndBottomNav() {
        mToolbar.isVisible = true
        bottomNavigation.isVisible = true
    }

}
