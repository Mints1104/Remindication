package com.mints.mobilehealthapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.BuildConfig
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.FragmentUserprofileBinding
import com.mints.mobilehealthapplication.viewmodels.ProfileViewModel

class ProfileFragment : Fragment() {
    private var _binding: FragmentUserprofileBinding? = null
    private val binding get() = _binding!!
    private var uid = ""
    private val viewModel: ProfileViewModel by viewModels()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var totalMedicationCount = 0
    private val mainActivity: MainActivity by lazy {
        requireActivity() as MainActivity
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserprofileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load user details
        viewModel.loadUserProfile()
       mainActivity.hideBottomNav()

        // Initialize ProfileViewModel functions
        viewModel.startListeningToAdherenceStreak()
        viewModel.checkMedicationHistory()
        viewModel.calculatePerfectWeeks()
         uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        viewModel.getMedications(uid)
        setupDebugMode()
        setupObservers()

        // Handle button clicks
        binding.btnChangePassword.setOnClickListener { changePassword() }
        binding.btnLogout.setOnClickListener { logoutUser() }
    }

    private fun setupObservers() {
        // Observe profile data and update UI
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            binding.profileFullName.text = "${user.firstName} ${user.lastName}"
            binding.profileEmail.text = user.email
            binding.profileDateOfBirth.text = user.dateOfBirth
            binding.profileAccountCreated.text = user.createdAt
            binding.profileUserId.text = user.uid
        }

        // Observe adherence streak
        viewModel.adherenceStreak.observe(viewLifecycleOwner) { streak ->
            binding.streakCount.text = streak.toString()

            // Update streak progress (7-day streak achievement)
            val progress = minOf(streak, 7)
            binding.streakProgress.progress = progress * 100 / 7
            binding.streakProgressText.text = "$progress/7"
        }

        // Observe if user has taken medication before
        viewModel.hasEverTakenMedication.observe(viewLifecycleOwner) { hasTaken ->
            // Update first medication badge (make it fully visible if achieved)
            binding.firstMedicationBadge.alpha = if (hasTaken) 1.0f else 0.5f
            binding.firstMedicationBadgeText.text = if (hasTaken) "✓" else "X"

        }

        // Observe perfect weeks count
        viewModel.perfectWeeks.observe(viewLifecycleOwner) { perfectWeeks ->
            binding.perfectWeekCount.text = "$perfectWeeks"
        }



        viewModel.totalDosesTaken.observe(viewLifecycleOwner) {
            binding.medicationMilestoneProgress.progress = it
            binding.medicationMilestoneProgressText.text = "$it/100"
        }


        viewModel.medications.observe(viewLifecycleOwner) { list ->
           totalMedicationCount = list.size
            binding.medicationCount.text = "$totalMedicationCount"
        }
        }






    private fun setupDebugMode() {
        // Only show debug toggle in debug builds
        if (BuildConfig.DEBUG) {
            binding.debugModeSwitch.visibility = View.VISIBLE
            binding.debugModeSwitch.setOnCheckedChangeListener { _, isChecked ->
                binding.debugUserIdContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }
    }

    private fun changePassword() {
        auth.sendPasswordResetEmail(auth.currentUser?.email ?: "").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                displayMessage("Password reset email sent")
            } else {
                displayMessage("Failed to send password reset email")
            }
        }
    }

    private fun logoutUser() {
        auth.signOut()
        displayMessage("Logged out")
        findNavController().navigate(R.id.global_action_to_loginFragment)
    }

    private fun displayMessage(msgTxt: String) {
        if(_binding == null) return
        Snackbar.make(binding.root, msgTxt, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}