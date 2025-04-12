package com.mints.mobilehealthapplication.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log // Import Log
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.mints.mobilehealthapplication.R

class SettingsFragment : PreferenceFragmentCompat() {
    private val mainActivity: MainActivity by lazy {
        requireActivity() as MainActivity
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val notificationSettingsLink: Preference? = findPreference("notification_settings_link")

        notificationSettingsLink?.setOnPreferenceClickListener {
            openAppSettingsNotificationPage()
            true
        }

    }

    private fun openAppSettingsNotificationPage() {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Failed to open specific notification settings, opening app details", e)
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            } catch (e2: Exception) {
                Log.e("SettingsFragment", "Failed to open app details settings as well", e2)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity.updateToolBarTitle("Settings")
    }
}