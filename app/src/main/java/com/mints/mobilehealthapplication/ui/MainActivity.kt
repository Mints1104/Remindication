package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R

class MainActivity : AppCompatActivity() {
    private lateinit var mToolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var navController: NavController
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        // Bind UI elements
        mToolbar = findViewById(R.id.main_toolbar)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        floatingActionButton = findViewById(R.id.add_medication_fab)

        // Set up NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up Toolbar with NavController
        setSupportActionBar(mToolbar)
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(R.id.homeFragment, R.id.profileFragment) // Add your top-level destinations here
//        )
//        setupActionBarWithNavController(navController, appBarConfiguration)

        // Set up BottomNavigationView with NavController
        bottomNavigation.setupWithNavController(navController)

        // Set up FloatingActionButton (optional)
        floatingActionButton.setOnClickListener {
            // Handle FAB click (e.g., navigate to a specific fragment)
        }

        // Check authentication state
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is logged in
            showAppBarAndBottomNav()
            retrieveUserInfo(currentUser.uid)
            navController.navigate(R.id.homeFragment) // Navigate to home fragment
        } else {
            // User is not logged in
            hideAppBarAndBottomNav()
            navController.navigate(R.id.loginFragment) // Navigate to login fragment
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

    fun showFAB() {
        floatingActionButton.isVisible = true
    }

    fun hideFAB() {
        floatingActionButton.isVisible = false
    }

    private fun hideAppBarAndBottomNav() {
        mToolbar.isVisible = false
        bottomNavigation.isVisible = false
    }

    private fun showAppBarAndBottomNav() {
        mToolbar.isVisible = true
        bottomNavigation.isVisible = true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}