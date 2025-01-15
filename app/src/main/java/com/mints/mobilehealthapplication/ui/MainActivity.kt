package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
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

        // Set up BottomNavigationView with NavController
        bottomNavigation.setupWithNavController(navController)

        // Set up FAB click listener
        floatingActionButton.setOnClickListener {
            // Handle FAB click
        }

        // Observe destination changes to update the Toolbar dynamically
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> {
                    updateToolbar(showBackArrow = false, showMenu = true, title = getString(R.string.app_name))
                }
                R.id.userInfoFragment -> {
                    updateToolbar(showBackArrow = false, showMenu = false, title = getString(R.string.app_name))
                }
                R.id.healthInfoFragment, R.id.medicationInfoFragment -> {
                    updateToolbar(showBackArrow = true, showMenu = false, title = getString(R.string.app_name))
                }
            }
        }

        // Check authentication state
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (navController.currentDestination?.id != R.id.homeFragment) {
                navController.navigate(R.id.homeFragment)
            }
        } else {
            if (navController.currentDestination?.id != R.id.loginFragment) {
                navController.navigate(R.id.loginFragment)
            }
        }
    }

    /**
     * Updates the Toolbar based on the current fragment's requirements.
     *
     * @param showBackArrow Whether to show the back arrow.
     * @param showMenu Whether to show the menu (settings and logout).
     * @param title The title to display in the Toolbar.
     */
    private fun updateToolbar(showBackArrow: Boolean, showMenu: Boolean, title: String) {
        mToolbar.title = title

        // Show or hide the back arrow
        if (showBackArrow) {
            mToolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
            mToolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        } else {
            mToolbar.navigationIcon = null
        }

        // Show or hide the menu
        if (showMenu) {
            mToolbar.inflateMenu(R.menu.top_app_bar)
        } else {
            mToolbar.menu.clear()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the Toolbar
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings_tab -> {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_logout -> {
                Toast.makeText(this, "Logout clicked", Toast.LENGTH_SHORT).show()
                auth.signOut()
                navController.navigate(R.id.action_homeFragment_to_loginFragment)
                true
            }

            R.id.streaks -> {
                val addStreakBottomSheet = StreakBottomSheetFragment()
                addStreakBottomSheet.show(supportFragmentManager, "StreakBottomSheetFragment")
                true
            }
            else -> super.onOptionsItemSelected(item)
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

    fun hideAppBar() {
        mToolbar.isVisible = false
    }

    fun showAppBar() {
        mToolbar.isVisible = true
    }

    fun showBottomNav() {
        bottomNavigation.isVisible = true
    }

    fun hideBottomNav() {
        bottomNavigation.isVisible = false
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}