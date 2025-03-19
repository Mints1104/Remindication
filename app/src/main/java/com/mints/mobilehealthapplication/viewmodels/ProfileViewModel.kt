package com.mints.mobilehealthapplication.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val dateOfBirth: String = "",
    val createdAt: String = "",
    val uid: String = ""
)

class ProfileViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> get() = _userProfile

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = UserProfile(
                    firstName = document.getString("firstName") ?: "",
                    lastName = document.getString("lastName") ?: "",
                    email = document.getString("email") ?: "",
                    dateOfBirth = document.getString("dateOfBirth") ?: "",
                    createdAt = document.getTimestamp("createdAt")?.toDate()?.toString() ?: "",
                    uid = userId
                )
                _userProfile.value = user
            }
    }
}
