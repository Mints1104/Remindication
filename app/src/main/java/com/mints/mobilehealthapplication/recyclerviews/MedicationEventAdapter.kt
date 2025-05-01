package com.mints.mobilehealthapplication.recyclerviews

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.databinding.MedicationEventItemBinding
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
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

        val eventDateTime: LocalDateTime =
            event.instant.atZone(ZoneId.of("UTC")).toLocalDateTime()

        Log.d("EventAdapter", "Stored Instant: ${event.instant}")
        Log.d("EventAdapter", "Local DateTime: $eventDateTime")

        when (event) {
            is MedicationEvent.Missed -> {
                holder.binding.eventDateText.text = getFormattedDateForMissed(eventDateTime)
            }
            else -> {
                holder.binding.eventDateText.text = getFormattedDate(eventDateTime)
            }
        }
    }

    private fun getFormattedDate(date: LocalDateTime): String {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val eventLocalDate = date.toLocalDate()
        return when (eventLocalDate) {
            today -> {
                val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
                "Today at ${date.format(timeFormat)}"
            }
            yesterday -> {
                val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
                "Yesterday at ${date.format(timeFormat)}"
            }
            else -> {
                val dateTimeFormat = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
                date.format(dateTimeFormat)
            }
        }
    }

    private fun getFormattedDateForMissed(date: LocalDateTime): String {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val eventLocalDate = date.toLocalDate()
        return when (eventLocalDate) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> {
                val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")
                date.format(dateFormat)
            }
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<MedicationEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }
}