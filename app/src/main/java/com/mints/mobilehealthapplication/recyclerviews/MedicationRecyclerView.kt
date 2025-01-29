package com.mints.mobilehealthapplication.recyclerviews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mints.mobilehealthapplication.R
import com.mints.mobilehealthapplication.data.Medication
import com.mints.mobilehealthapplication.data.MedicationDiffCallback
import com.mints.mobilehealthapplication.data.MedicationSchedule
import com.mints.mobilehealthapplication.databinding.ItemMedicationBinding

class MedicationRecyclerView(
    private var medications: List<Medication>,
    private val onClick: (Medication) -> Unit
) : RecyclerView.Adapter<MedicationRecyclerView.MedicationViewHolder>() {

    private val tag = "RecyclerView"
    class MedicationViewHolder(val binding: ItemMedicationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
    //    Log.d(tag, "Creating ViewHolder")
        val binding = ItemMedicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medication = medications[position]
       // Log.d(tag, "Binding medication: ${medication.name}")
    //    Log.d(tag, "Binding medication: ${medication.id}")

        val context = holder.binding.root.context

        holder.binding.medicationName.text = medication.name
        holder.binding.medicationId.text = context.getString(R.string.id,medication.id)
        holder.binding.medicationDosage.text = context.getString(R.string.dosage_of_medication, medication.dosage)
        holder.binding.medicationFrequency.text = context.getString(R.string.frequency_of_medication,medication.schedule.formattedFrequency)

        val medicationSchedule = medication.schedule



        if(medicationSchedule is MedicationSchedule.Daily) {
            holder.binding.medicationDay.text = context.getString(R.string.medication_date,medicationSchedule.nextDueDates)

        }

        if(medicationSchedule is MedicationSchedule.WeeklySchedule) {
            holder.binding.medicationDay.text = context.getString(R.string.medication_date,medicationSchedule.nextDueDates)
        }

        if(medication.schedule.formattedFrequency ==  "As Needed") {
            holder.binding.medicationTime.text = context.getString(R.string.empty_string)
        } else {
            holder.binding.medicationTime.text = context.getString(R.string.time_of_medication,medication.schedule.formattedTimes)

        }


        holder.binding.root.setOnClickListener {
                        onClick(medication)
        }
    }

    override fun getItemCount(): Int {
        return medications.size
    }

    fun getMedicationList(): List<Medication> {
        return medications
    }


    fun getMedicationAt(position: Int): Medication {
        return medications[position]
    }

    fun getMedicationNameAt(position: Int): String {
        return medications[position].name
    }

    fun updateMedicationList(newMedications: List<Medication>) {
        val diffCallback = MedicationDiffCallback(medications, newMedications)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        medications = newMedications
        diffResult.dispatchUpdatesTo(this)
    }

}