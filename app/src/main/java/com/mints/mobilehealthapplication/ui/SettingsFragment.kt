package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.mints.mobilehealthapplication.R

class SettingsFragment : PreferenceFragmentCompat() {
    private val mainActivity: MainActivity by lazy {
        requireActivity() as MainActivity
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity.updateToolBarTitle("Settings")

    }
}