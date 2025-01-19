package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R

class MainActivity : AppCompatActivity() {
    private lateinit var mToolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var floatingActionButton: ExtendedFloatingActionButton
    private lateinit var navController: NavController
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var shouldShowMenu = false

    // Tracks current toolbar menu to prevent duplicate inflation
    private var currentMenu: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeFirebase()
        bindUIElements()
        setupNavigation()
        checkAuthenticationState()
    }

    private fun initializeFirebase() {
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
    }

    private fun bindUIElements() {
        mToolbar = findViewById(R.id.main_toolbar)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        floatingActionButton = findViewById(R.id.add_medication_fab)

        setSupportActionBar(mToolbar)

        floatingActionButton.setOnClickListener {
            // Handle FAB click
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        bottomNavigation.setupWithNavController(navController)

        // Update UI elements whenever navigation destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateUIForDestination(destination.id)
        }
    }

    private fun updateUIForDestination(destinationId: Int) {
        when (destinationId) {
            // Main dashboard with full navigation
            R.id.homeFragment -> {
                showAllUI()
                updateToolbar(
                    showBackArrow = false,
                    menuResId = R.menu.top_app_bar,
                    title = getString(R.string.app_name)
                )
                invalidateOptionsMenu()  // Force menu refresh

            }
            // Profile view without additional navigation
            R.id.userInfoFragment -> {
                hideFAB()
                hideBottomNav()
                updateToolbar(
                    showBackArrow = false,
                    menuResId = null,
                    title = getString(R.string.app_name)
                )
            }
            // Detail views with back navigation
            R.id.healthInfoFragment -> {
                hideFAB()
                hideBottomNav()
                updateToolbar(
                    showBackArrow = true,
                    menuResId = null,
                    title = getString(R.string.app_name)
                )
            }
            // Authentication view without navigation elements
            R.id.loginFragment, R.id.resetPasswordFragment -> {
                hideAllUI()
            }

            R.id.addMedicationBasicInfoFragment -> {
                // Choose UI state:
                showAllUI() // or hideAllUI() or mix of show/hide methods
                updateToolbar(
                    showBackArrow = true,
                    menuResId =   null,
                    title = getString(R.string.add_medication_txt)
                )
            }

            // Template for new fragments
            /*
            R.id.newFragment -> {
                // Choose UI state:
                showAllUI() // or hideAllUI() or mix of show/hide methods
                updateToolbar(
                    showBackArrow = true/false,
                    menuResId = R.menu.your_menu or null,
                    title = "Your Title"
                )
            }
            */
        }
    }

     fun showAllUI() {
        showAppBar()
        showBottomNav()
        showFAB()
    }

     fun hideAllUI() {
        hideAppBar()
        hideBottomNav()
        hideFAB()
    }

    private fun updateToolbar(showBackArrow: Boolean, menuResId: Int?, title: String) {
        mToolbar.title = title

        // Configure back navigation
        if (showBackArrow) {
            mToolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
            mToolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed()
            }
        } else {
            mToolbar.navigationIcon = null
        }

        // Store the menu resource ID for later use
        currentMenu = menuResId
        shouldShowMenu = menuResId != null

        // Force menu recreation
        invalidateOptionsMenu()
    }

    private fun checkAuthenticationState() {
        val currentUser = auth.currentUser
        if (currentUser == null && navController.currentDestination?.id != R.id.loginFragment) {
            navController.navigate(R.id.loginFragment)
        } else if (currentUser != null && navController.currentDestination?.id != R.id.homeFragment) {
            navController.navigate(R.id.homeFragment)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings_tab -> {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_logout -> {
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

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Only inflate the menu if it should be shown
        if (shouldShowMenu && currentMenu != null) {
            menuInflater.inflate(currentMenu!!, menu)
            return true
        }
        return false
    }

    // UI visibility control methods
    fun showFAB() { floatingActionButton.isVisible = true }
    fun hideFAB() { floatingActionButton.isVisible = false }
    fun hideAppBar() { mToolbar.isVisible = false }
    fun showAppBar() { mToolbar.isVisible = true }
    fun showBottomNav() { bottomNavigation.isVisible = true }
    fun hideBottomNav() { bottomNavigation.isVisible = false }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}