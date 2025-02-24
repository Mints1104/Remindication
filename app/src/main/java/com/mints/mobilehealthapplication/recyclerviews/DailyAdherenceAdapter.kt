package com.mints.mobilehealthapplication.recyclerviews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mints.mobilehealthapplication.databinding.DailyAdherenceItemBinding
import java.time.LocalDate

class DailyAdherenceAdapter(private var items: List<Pair<LocalDate, String>>) : RecyclerView.Adapter<DailyAdherenceAdapter.DailyAdherenceViewHolder>() {

    inner class DailyAdherenceViewHolder(val binding: DailyAdherenceItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyAdherenceViewHolder {
        val binding = DailyAdherenceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DailyAdherenceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyAdherenceViewHolder, position: Int) {
        val (date, status) = items[position]
        holder.binding.dayDateText.text = date.toString() // You can format this as needed
        holder.binding.dayStatusText.text = status
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Pair<LocalDate, String>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
