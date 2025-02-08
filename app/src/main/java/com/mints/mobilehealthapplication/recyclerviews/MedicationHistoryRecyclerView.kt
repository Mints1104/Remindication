package com.mints.mobilehealthapplication.recyclerviews

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationDiffCallback
import com.mints.mobilehealthapplication.databinding.ItemMedhistoryBinding

class MedicationHistoryRecyclerView(
    private var medications: List<Medication>,
    private val onClick: (Medication) -> Unit
) : RecyclerView.Adapter<MedicationHistoryRecyclerView.MedicationViewHolder>() {

    companion object {
        private const val TAG = "MedicationHistoryRV"
    }

    class MedicationViewHolder(val binding: ItemMedhistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        Log.d(TAG, "onCreateViewHolder: Creating a new ViewHolder")
        val binding =
            ItemMedhistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medication = medications[position]
        Log.d(TAG, "onBindViewHolder: Binding medication at position $position: ${medication.name}")

        holder.binding.medicationName.text = medication.name

        // Set the click listener.
        holder.binding.root.setOnClickListener {
            Log.d(TAG, "onBindViewHolder: ${medication.name} clicked")
            onClick(medication)
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: Total items = ${medications.size}")
        return medications.size
    }

    fun getMedicationList(): List<Medication> = medications

    fun getMedicationAt(position: Int): Medication = medications[position]

    fun getMedicationNameAt(position: Int): String = medications[position].name

    fun updateMedicationList(newMedications: List<Medication>) {
        Log.d(TAG, "updateMedicationList: Updating list. Old size = ${medications.size}, New size = ${newMedications.size}")
        val diffCallback = MedicationDiffCallback(medications, newMedications)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        medications = newMedications
        diffResult.dispatchUpdatesTo(this)
        Log.d(TAG, "updateMedicationList: Medication list updated")
    }
}
