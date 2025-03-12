// File: java/com/mints/mobilehealthapplication/ui/MedicationTrendsFragment.kt
package com.mints.mobilehealthapplication.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.mints.mobilehealthapplication.data.MedicationEvent
import com.mints.mobilehealthapplication.databinding.FragmentMedicationTrendsBinding
import com.mints.mobilehealthapplication.viewmodels.MedicationAnalyticsViewModel
import java.time.LocalDate

class MedicationTrendsFragment : Fragment() {
    private var _binding: FragmentMedicationTrendsBinding? = null
    private val binding get() = _binding!!
    private var uid = ""

    private val viewModel: MedicationAnalyticsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicationTrendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (uid.isNotEmpty()) {
            viewModel.getMedications(uid)
            viewModel.medications.observe(viewLifecycleOwner) { medications ->
                if (medications.isNotEmpty()) {
                    val events: List<MedicationEvent> =
                        medications.flatMap { it.medicationHistory.getAllEvents() }
                    setupAdherenceChart(events)
                    setupWeeklyChart(events)
                    setupDailyTimingChart(events)
                }
            }
        }
    }

    private fun setupAdherenceChart(events: List<MedicationEvent>) {
        val chart = binding.adherenceChart
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.legend.isEnabled = true

        val eventsByDate: Map<String, List<MedicationEvent>> = events.groupBy { event ->
            event.date.toLocalDate().toString()
        }.toSortedMap()

        val entries: List<Entry> = eventsByDate.entries.mapIndexed { index, entry ->
            val dateEvents: List<MedicationEvent> = entry.value
            val total: Int = dateEvents.size
            val taken: Int = dateEvents.count { it.type == MedicationEvent.EventType.TAKEN }
            val value: Float = if (total > 0) taken.toFloat() / total * 100 else 0f
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "Daily Adherence")
        dataSet.color = Color.BLUE
        dataSet.lineWidth = 2f
        dataSet.setCircleColor(Color.BLUE)
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawValues(false)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(
            eventsByDate.keys.map { key ->
                val date = LocalDate.parse(key)
                "${date.monthValue}/${date.dayOfMonth}"
            }
        )
        chart.data = LineData(dataSet)
        chart.invalidate()
    }

    private fun setupWeeklyChart(events: List<MedicationEvent>) {
        val chart = binding.weeklyChart
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.legend.isEnabled = true

        val dayNames: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val eventsByDay: Map<Int, List<MedicationEvent>> = events.groupBy { event ->
            val date = event.date.toLocalDate()
            date.dayOfWeek.value // 1 (Monday) to 7 (Sunday)
        }

        val entries: List<BarEntry> = (1..7).map { dayOfWeek ->
            val dayEvents: List<MedicationEvent> = eventsByDay[dayOfWeek] ?: emptyList()
            BarEntry((dayOfWeek - 1).toFloat(), dayEvents.size.toFloat())
        }

        val dataSet = BarDataSet(entries, "Events by Day")
        dataSet.color = Color.GREEN

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(dayNames)

        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
        chart.invalidate()
    }

    private fun setupDailyTimingChart(events: List<MedicationEvent>) {
        val chart = binding.dailyTimingChart
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.legend.isEnabled = true

        val eventsByHour: Map<Int, List<MedicationEvent>> = events.groupBy { event ->
            event.date.hour
        }

        val timeBlocks: Map<String, List<Int>> = mapOf(
            "Morning (6-11)" to (6..11).toList(),
            "Afternoon (12-17)" to (12..17).toList(),
            "Evening (18-23)" to (18..23).toList(),
            "Night (0-5)" to (0..5).toList()
        )

        val entries: List<BarEntry> = timeBlocks.entries.mapIndexed { index, entry ->
            val count: Int = entry.value.sumOf { hour -> eventsByHour[hour]?.size ?: 0 }
            BarEntry(index.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "Events by Time")
        dataSet.color = Color.CYAN

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(timeBlocks.keys.toList())

        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}