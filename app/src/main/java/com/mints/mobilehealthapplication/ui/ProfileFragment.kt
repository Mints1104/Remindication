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
import com.google.firebase.firestore.FirebaseFirestore
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.databinding.FragmentUserprofileBinding
import com.mints.mobilehealthapplication.viewmodels.ProfileViewModel

class ProfileFragment : Fragment() {
    private var _binding: FragmentUserprofileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

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
        (requireActivity() as MainActivity).hideBottomNav()

        // Observe profile data and update UI
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            binding.profileFullName.text = "${user.firstName} ${user.lastName}"
            binding.profileEmail.text = user.email
            binding.profileDateOfBirth.text = user.dateOfBirth
            binding.profileAccountCreated.text = user.createdAt
            binding.profileUserId.text = user.uid
        }

        // Handle button clicks
        binding.btnChangePassword.setOnClickListener { changePassword() }
        binding.btnLogout.setOnClickListener { logoutUser() }
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
        Snackbar.make(binding.root, msgTxt, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
