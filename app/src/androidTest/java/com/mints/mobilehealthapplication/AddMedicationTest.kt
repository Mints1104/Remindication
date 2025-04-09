package com.mints.mobilehealthapplication

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.ui.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddMedicationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        // Sign out Firebase to ensure LoginFragment loads
        FirebaseAuth.getInstance().signOut()
        println("Signed out Firebase user")
        Thread.sleep(2000) // Wait for auth state to settle
    }

    @Test
    fun testLoginAndNavigateToHome() {
        println("Starting test: Logging in...")

        // Step 1: Log in via LoginFragment
        onView(withId(R.id.email_edit_text))
            .perform(typeText("testuser@example.com"))
        onView(withId(R.id.password_edit_text))
            .perform(typeText("password123"))
        onView(withId(R.id.login_button))
            .perform(click())

        // Wait for auth and navigation
        Thread.sleep(10000)

        // Step 2: Check FAB on HomeFragment
        onView(withId(R.id.add_medication_fab))
            .check(matches(isDisplayed()))
    }
}