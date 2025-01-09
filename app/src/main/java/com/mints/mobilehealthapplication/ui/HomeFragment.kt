package com.mints.mobilehealthapplication.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.recyclerviews.MedicationsAdapter

class HomeFragment : Fragment() {

    private lateinit var db: FirebaseFirestore

    private lateinit var recyclerView: RecyclerView
    private lateinit var nextMedicationName: TextView
    private lateinit var nextMedicationTime: TextView
    private lateinit var medicationsAdapter: MedicationsAdapter

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser
        recyclerView = rootView.findViewById(R.id.medications_recycler_view)
        nextMedicationName = rootView.findViewById(R.id.next_medication_name)
        nextMedicationTime = rootView.findViewById(R.id.next_medication_time)

        setupRecyclerView()
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.add_medication_fab)
        fab.setOnClickListener {
            showAddMedicationDialog()
        }

        // Load initial data
        loadMedications()
        updateNextMedicationCard()





        return rootView
    }

    private fun setupRecyclerView() {
        medicationsAdapter = MedicationsAdapter(emptyList()) { medication ->
            // Handle medication item click
            showMedicationDetails(medication)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = medicationsAdapter
        }
    }
    private fun loadMedications() {
        // TODO: Load medications from your data source (Room database, etc.)
        // For now, we'll use dummy data
        val dummyMedications = listOf(
            Medication("Aspirin", "08:00 AM", "Daily"),
            Medication("Vitamin D", "09:00 AM", "Daily"),
            Medication("Ibuprofen", "02:00 PM", "As needed")
        )
        medicationsAdapter.updateMedications(dummyMedications)
    }
    private fun updateNextMedicationCard() {
        // TODO: Calculate and display the next medication due
        // For now, we'll use dummy data
        nextMedicationName.text = "Aspirin"
        nextMedicationTime.text = "Next dose: Today at 08:00 AM"
    }

    private fun showAddMedicationDialog() {
        // TODO: Implement add medication dialog
        // This would typically launch a dialog or navigate to an add medication screen
    }

    private fun showMedicationDetails(medication: Medication) {
        // TODO: Implement medication details view
        // This would typically navigate to a detail screen for the selected medication
    }


    override fun onResume() {
        super.onResume()
        Log.d("LoginFragment","In Onresume...")
    }

    private fun displayMessage(view: View, msgTxt: String) {
        Snackbar.make(view, msgTxt, Snackbar.LENGTH_SHORT).show()
    }
}
