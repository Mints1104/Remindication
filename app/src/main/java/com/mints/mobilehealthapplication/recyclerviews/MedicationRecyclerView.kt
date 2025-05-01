package com.mints.mobilehealthapplication.recyclerviews

import android.view.LayoutInflater
import android.view.View
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

    private var hideMedicationDays: Boolean = false

    class MedicationViewHolder(val binding: ItemMedicationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val binding =
            ItemMedicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medication = medications[position]
        val context = holder.binding.root.context

        holder.binding.medicationName.text = medication.name
        holder.binding.medicationDosage.text = context.getString(R.string.dosage_of_medication, medication.dosage)
        holder.binding.medicationFrequency.text = context.getString(R.string.frequency_of_medication, medication.schedule.frequencyType)

        val medicationSchedule = medication.schedule

        if (medicationSchedule is MedicationSchedule.OnDemand) {
            holder.binding.medicationDay.visibility = View.GONE
            holder.binding.medicationTime.visibility = View.GONE
            holder.binding.medicationDay.text = context.getString(R.string.empty_string)
            holder.binding.medicationTime.text = context.getString(R.string.empty_string)
        } else {
            holder.binding.medicationTime.visibility = View.VISIBLE
            holder.binding.medicationTime.text = context.getString(R.string.time_of_medication, medication.schedule.formattedTimes)

            if (hideMedicationDays) {
                holder.binding.medicationDay.visibility = View.GONE
            } else {
                holder.binding.medicationDay.visibility = View.VISIBLE

                if (medicationSchedule is MedicationSchedule.Daily) {

                    holder.binding.medicationDay.text = context.getString(
                        R.string.medication_date,
                        medicationSchedule.nextDueDates[0].dayOfWeek.toString()
                            .lowercase()
                            .replaceFirstChar { it.uppercase() }
                    )
                }
                if (medicationSchedule is MedicationSchedule.WeeklySchedule) {
                    holder.binding.medicationDay.text = context.getString(
                        R.string.medication_date,
                        medicationSchedule.nextDueDates.joinToString(", ") { date ->
                            date.dayOfWeek.toString().lowercase().replaceFirstChar { it.uppercase() }
                        }
                    )
                }

            }
        }

        holder.binding.root.setOnClickListener {
            onClick(medication)
        }
    }

    override fun getItemCount(): Int = medications.size

    fun getMedicationList(): List<Medication> = medications

    fun getMedicationAt(position: Int): Medication = medications[position]

    fun getMedicationNameAt(position: Int): String = medications[position].name

    fun updateMedicationList(newMedications: List<Medication>) {
        val diffCallback = MedicationDiffCallback(medications, newMedications)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        medications = newMedications
        diffResult.dispatchUpdatesTo(this)
    }


    fun hideAllMedicationDays() {
        hideMedicationDays = true
        notifyDataSetChanged()
    }
}
