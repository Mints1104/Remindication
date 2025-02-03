package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.mints.mobilehealthapplication.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}