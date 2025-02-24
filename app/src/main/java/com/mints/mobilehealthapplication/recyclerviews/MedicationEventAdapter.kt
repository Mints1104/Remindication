package com.mints.mobilehealthapplication.recyclerviews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.databinding.MedicationEventItemBinding
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
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        holder.binding.eventDateText.text = event.date.format(formatter)
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<MedicationEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
