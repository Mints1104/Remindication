package com.mints.mobilehealthapplication.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R

class MainActivity : AppCompatActivity() {
    private lateinit var mToolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        auth = Firebase.auth
        val currentUser = auth.currentUser
        mToolbar = findViewById(R.id.main_toolbar)
        if (currentUser != null) {
            loadFragment(HomeFragment())
            showAppBarAndBottomNav()

        } else {
            loadFragment(LoginFragment())
            hideAppBarAndBottomNav()

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
