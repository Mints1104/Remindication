package com.mints.mobilehealthapplication.recyclerviews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.Medication

class MedicationRecyclerView(private var medications: List<Medication>)
    : RecyclerView.Adapter<MedicationRecyclerView.MedicationViewHolder>() {

    // ViewHolder for each medication item
    class MedicationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.medication_name)
        val dosageTextView: TextView = view.findViewById(R.id.medication_dosage)
        val frequencyTextView: TextView = view.findViewById(R.id.medication_frequency)
        val timeTextView: TextView = view.findViewById(R.id.medication_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medication, parent, false)
        return MedicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medication = medications[position]

        // Bind the medication data to the corresponding TextViews
        holder.nameTextView.text = medication.name
        holder.dosageTextView.text = medication.dosage
        holder.frequencyTextView.text = medication.frequency
        holder.timeTextView.text = medication.time
    }

    override fun getItemCount(): Int = medications.size

    // Optionally, you can add a method to update the list of medications if needed
    fun updateMedicationList(newMedications: List<Medication>) {
        medications = newMedications
        notifyDataSetChanged()
    }
}
