package com.mints.mobilehealthapplication.recyclerviews

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationDiffCallback
import com.mints.mobilehealthapplication.databinding.ItemMedicationBinding

class MedicationRecyclerView(
    private var medications: List<Medication>,
    private val onClick: (Medication) -> Unit
) : RecyclerView.Adapter<MedicationRecyclerView.MedicationViewHolder>() {

    class MedicationViewHolder(val binding: ItemMedicationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        Log.d("MedicationRecyclerView", "Creating ViewHolder")
        val binding = ItemMedicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medication = medications[position]
        Log.d("MedicationRecyclerView", "Binding medication: ${medication.name}")
        Log.d("MedicationRecyclerView", "Binding medication: ${medication.id}")

        val context = holder.binding.root.context

        holder.binding.medicationName.text = medication.name
        holder.binding.medicationId.text = medication.id
        holder.binding.medicationDosage.text = context.getString(R.string.dosage_of_medication, medication.dosage)
        holder.binding.medicationFrequency.text = context.getString(R.string.frequency_of_medication,medication.schedule.formattedFrequency)
        holder.binding.medicationTime.text = context.getString(R.string.time_of_medication,medication.schedule.formattedTimes)

        holder.binding.root.setOnClickListener {
                        onClick(medication)
        }
    }

    override fun getItemCount(): Int {
        return medications.size
    }

    fun updateMedicationList(newMedications: List<Medication>) {
        val diffCallback = MedicationDiffCallback(medications, newMedications)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        medications = newMedications
        diffResult.dispatchUpdatesTo(this)
    }

}