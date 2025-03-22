package com.mints.mobilehealthapplication.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.InternetConnectionChecker
import com.mints.mobilehealthapplication.data.NotificationHelper
import com.mints.mobilehealthapplication.viewmodels.HomeFragmentViewModelFactory

class MainActivity : AppCompatActivity() {
    private lateinit var mToolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var floatingActionButton: ExtendedFloatingActionButton
    private lateinit var navController: NavController
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var shouldShowMenu = false
    private lateinit var alarmManager: AlarmManager
    lateinit var internetChecker: InternetConnectionChecker
        private var currentMenu: Int? = null

    val homeFragmentViewModelFactory: HomeFragmentViewModelFactory by lazy {
        HomeFragmentViewModelFactory(NotificationHelper(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeFirebase()
        bindUIElements()
        setupNavigation()
        checkAuthenticationState()
        requestNotificationPermission()
        setUpAlarmManager()
        internetChecker = InternetConnectionChecker(applicationContext)
        internetChecker.registerNetworkCallback()

        val initialConnectionStatus = internetChecker.checkInternetConnection()
        if(initialConnectionStatus) {
            Log.d("MainActivity","Initial internet check is true")
        } else {
            Log.d("MainActivity","Initial internet check is false")

        }



    }

    fun checkNetworkState(): Boolean {
        return internetChecker.isConnected
    }

        private fun setUpAlarmManager() {
        Log.d("AlarmDebug", "Setting up alarm manager")
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                Log.d("AlarmDebug", "Exact alarm permission already granted")
            } else {
                Log.w("AlarmDebug", "Exact alarm permission NOT granted")
                requestPermission()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private val alarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (alarmManager.canScheduleExactAlarms()) {
            Log.d("AlarmDebug", "Exact alarm permission granted")
        } else {
            Log.w("AlarmDebug", "Exact alarm permission denied")
            Toast.makeText(
                this,
                "Permission denied. Using fallback method.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestPermission() {
        try {
            alarmPermissionLauncher.launch(Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        } catch (e: Exception) {
            Log.e("AlarmDebug", "Permission request failed", e)
            Toast.makeText(this, "Error requesting permission", Toast.LENGTH_SHORT).show()
        }
    }



    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
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
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateUIForDestination(destination.id)
        }
    }

    private fun updateUIForDestination(destinationId: Int) {
        when (destinationId) {
            R.id.homeFragment -> {
                showAllUI()
                updateToolbar(
                    showBackArrow = false,
                    menuResId = R.menu.top_app_bar,
                    title = getString(R.string.app_name)
                )
                invalidateOptionsMenu()
            }
            R.id.prescriptionsFragment -> {
                hideFAB()
                updateToolbar(
                    showBackArrow = false,
                    menuResId = R.menu.top_app_bar,
                    title = "Prescriptions"
                )
                invalidateOptionsMenu()
            }

            R.id.medicationHistoryFragment -> {
                hideFAB()
                updateToolbar(
                    showBackArrow = false,
                    menuResId = R.menu.top_app_bar,
                    title = "Medication History"
                )
                invalidateOptionsMenu()
            }
            R.id.userInfoFragment -> {
                hideFAB()
                hideBottomNav()
                updateToolbar(
                    showBackArrow = false,
                    menuResId = null,
                    title = getString(R.string.app_name)
                )
            }
            R.id.healthInfoFragment,R.id.settingsFragment -> {
                hideFAB()
                hideBottomNav()
                updateToolbar(
                    showBackArrow = true,
                    menuResId = null,
                    title = getString(R.string.app_name)
                )
            }
            R.id.loginFragment, R.id.resetPasswordFragment -> {
                hideAllUI()
            }
            R.id.addMedicationBasicInfoFragment, R.id.addMedicationFrequencyFragment -> {
                showAllUI()
                updateToolbar(
                    showBackArrow = true,
                    menuResId =   null,
                    title = getString(R.string.add_medication_txt)
                )
            }
            R.id.medicationDetailFragment -> {
                hideFAB()
                updateToolbar(showBackArrow = true,
                    menuResId = R.menu.top_app_bar,
                    title = "Medication History")
            }
            R.id.medicationTrendsFragment -> {
                hideFAB()
                updateToolbar(
                    showBackArrow = true,
                    menuResId = R.menu.top_app_bar,
                    title = "Medication Trends"
                )
            }
            R.id.userProfileFragment -> {
                hideFAB()
                updateToolbar(
                    showBackArrow = true,
                    menuResId = null,
                    title = "Profile"
                )
            }

            R.id.medicationInfoFragment -> {
                hideFAB()
                updateToolbar(
                    showBackArrow = true,
                    menuResId = null,
                    title = "Medication Info"
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


    fun updateToolBarTitle(title:String) {
        mToolbar.title = title
    }


    private fun updateToolbar(showBackArrow: Boolean, menuResId: Int?, title: String) {
        mToolbar.title = title
        if (showBackArrow) {
            mToolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
            mToolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed()
            }
        } else {
            mToolbar.navigationIcon = null
        }

        currentMenu = menuResId
        shouldShowMenu = menuResId != null

        invalidateOptionsMenu()
    }


    private fun checkAuthenticationState() {
        val currentUser = auth.currentUser
        val currentDestination = navController.currentDestination?.id
        if (currentUser == null && currentDestination != R.id.loginFragment) {
            navController.navigate(R.id.loginFragment)
        } else if (currentUser != null && currentDestination != R.id.homeFragment) {
            navController.navigate(R.id.homeFragment, null, navOptions {
                popUpTo(R.id.loginFragment) { inclusive = true }
                launchSingleTop = true
            })
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings_tab -> {
                navController.navigate(R.id.global_action_to_settingsFragment)
                true
            }
            R.id.profile_tab -> {
                navController.navigate(R.id.global_action_to_userProfileFragment)
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


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (shouldShowMenu && currentMenu != null) {
            menuInflater.inflate(currentMenu!!, menu)
            return true
        }
        return false
    }


    private fun showFAB() { floatingActionButton.isVisible = true }
    fun hideFAB() { floatingActionButton.isVisible = false }
    fun hideAppBar() { mToolbar.isVisible = false }
    fun showAppBar() { mToolbar.isVisible = true }
    fun showBottomNav() { bottomNavigation.isVisible = true }
    fun hideBottomNav() { bottomNavigation.isVisible = false }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        internetChecker.unregisterNetworkCallback()
        super.onDestroy()
    }
}