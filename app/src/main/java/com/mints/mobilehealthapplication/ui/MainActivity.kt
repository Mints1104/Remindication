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
    private val REQUEST_PERMISSION_CODE = 1001

    private var currentMenu: Int? = null

    val homeFragmentViewModelFactory: HomeFragmentViewModelFactory by lazy {
        HomeFragmentViewModelFactory(NotificationHelper(this))
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
    }


    private fun setUpAlarmManager() {
        Log.d("AlarmDebug", "Setting up alarm manager")
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                Log.d("AlarmDebug", "Exact alarm permission already granted")
            //    scheduleExactAlarm()
            } else {
                Log.w("AlarmDebug", "Exact alarm permission NOT granted")
                requestPermission()
            }
        } else {
          //  scheduleExactAlarm()
        }

    }


    @RequiresApi(Build.VERSION_CODES.S)
    private val alarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (alarmManager.canScheduleExactAlarms()) {
            Log.d("AlarmDebug", "Exact alarm permission granted")
            //scheduleExactAlarm()
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

//    private fun scheduleExactAlarm() {
//        val triggerTime = System.currentTimeMillis() + 5000
//        Log.d("AlarmDebug", "Scheduling exact alarm at ${Date(triggerTime)}")
//        Log.d("AlarmDebug", "Creating PendingIntent with requestCode 0")
//
//        // Step 1: Create the intent to trigger when the alarm goes off
//        val intent = Intent(this, NotificationReceiver::class.java).apply {
//            // Add any extras if needed, like medication details
//            putExtra("medication_name", "Aspirin")
//            putExtra("dosage", "500mg")
//        }
//
//        // Step 2: Create the PendingIntent to trigger the notification
//        val pendingIntent = PendingIntent.getBroadcast(
//            this,
//            0,  // You can use a unique request code here
//            intent,
//            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//        )
//
//        // Step 3: Schedule the alarm with AlarmManager using setExact
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            alarmManager.setExact(
//                AlarmManager.RTC_WAKEUP,  // Wake the device if it's asleep
//                triggerTime,  // Trigger time in milliseconds
//                pendingIntent  // PendingIntent that will be triggered
//            )
//        } else {
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
//        }
//
//        Log.d("Alarm", "Exact alarm set for: $triggerTime")
//    }


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
                    title = getString(R.string.app_name)
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
                navController.navigate(R.id.action_homeFragment_to_settingsFragment)
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
}