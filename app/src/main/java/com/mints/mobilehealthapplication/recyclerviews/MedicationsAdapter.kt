package com.mints.mobilehealthapplication.recyclerviews


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.Medication

class MedicationsAdapter(
    private var medications: List<Medication>,
    private val onItemClick: (Medication) -> Unit
) : RecyclerView.Adapter<MedicationsAdapter.MedicationViewHolder>() {

    class MedicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.medication_name)
        val timeText: TextView = itemView.findViewById(R.id.medication_time)
        val frequencyText: TextView = itemView.findViewById(R.id.medication_frequency)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medication, parent, false)
        return MedicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medication = medications[position]
        holder.nameText.text = medication.name
        holder.timeText.text = medication.time
        holder.frequencyText.text = medication.frequency

        holder.itemView.setOnClickListener {
            onItemClick(medication)
        }
    }

    override fun getItemCount() = medications.size

    fun updateMedications(newMedications: List<Medication>) {
        medications = newMedications
        notifyDataSetChanged()
    }
}