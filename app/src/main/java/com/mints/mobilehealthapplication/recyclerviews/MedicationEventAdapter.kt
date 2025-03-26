package com.mints.mobilehealthapplication.recyclerviews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.databinding.MedicationEventItemBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MedicationEventAdapter(private var events: List<MedicationEvent>) :
    RecyclerView.Adapter<MedicationEventAdapter.EventViewHolder>() {

    inner class EventViewHolder(val binding: MedicationEventItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = MedicationEventItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.binding.eventTypeText.text = event.type.name
        when(event) {
            is MedicationEvent.Missed -> {
                holder.binding.eventDateText.isVisible = false
            }
            else -> {
                holder.binding.eventDateText.text = event.getFormattedDate()

            }

        }
    }

    private fun MedicationEvent.getFormattedDate(): String {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        return when (this.date.toLocalDate()) {
            today -> "Today at ${this.date.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            yesterday -> "Yesterday at ${this.date.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            else -> this.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
        }
    }

    private fun MedicationEvent.getFormattedDateForMissed(): String {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        return when (this.date.toLocalDate()) {
            today -> "Today at ${this.date.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            yesterday -> "Yesterday at ${this.date.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            else -> this.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<MedicationEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
