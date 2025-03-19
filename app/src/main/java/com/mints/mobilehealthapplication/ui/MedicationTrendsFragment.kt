// File: java/com/mints/mobilehealthapplication/ui/MedicationTrendsFragment.kt
package com.mints.mobilehealthapplication.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
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
    private val args: MedicationTrendsFragmentArgs by navArgs()
    private var tag = "MedicationTrendsFragment"

    private val viewModel: MedicationAnalyticsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicationTrendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val medicationId = args.medicationId
        Log.d(tag, "Passed in medicationID: $medicationId")

        uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (uid.isEmpty()) return

        viewModel.getMedications(uid)
        viewModel.medications.observe(viewLifecycleOwner) { medications ->
            if (medications.isEmpty()) return@observe

            val events: List<MedicationEvent> = if (medicationId.isNullOrEmpty()) {
                Log.d(tag, "Medication ID is null or empty")
                medications.flatMap { it.medicationHistory.getAllEvents() }
            } else {
                val medication = medications.find { it.id == medicationId }
                Log.d(tag, "Medication Name: ${medication?.name}")
                // Update the toolbar title using the medication name
                (requireActivity() as MainActivity).updateToolBarTitle("${medication!!.name} Trends")
                medication.medicationHistory.getAllEvents()
            }

            setupAdherenceChart(events)
            setupWeeklyChart(events)
            setupDailyTimingChart(events)
        }
    }


    private fun setupAdherenceChart(events: List<MedicationEvent>) {
        val chart = binding.adherenceChart
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.legend.isEnabled = true

        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val labelColor = if (isDarkMode) Color.WHITE else Color.BLACK

        // Set X-Axis text color
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.textColor = labelColor
        xAxis.valueFormatter = IndexAxisValueFormatter(
            events.groupBy { it.date.toLocalDate().toString() }
                .toSortedMap()
                .keys.map { key ->
                    val date = LocalDate.parse(key)
                    "${date.monthValue}/${date.dayOfMonth}"
                }
        )

        // Set left Y-Axis and right Y-Axis text colors
        chart.axisLeft.textColor = labelColor
        chart.axisRight.textColor = labelColor

        // Set legend text color
        chart.legend.textColor = labelColor

        // Prepare your data set
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

        val dataSet = LineDataSet(entries, "Daily Adherence").apply {
            color = Color.BLUE
            lineWidth = 2f
            setCircleColor(Color.BLUE)
            setDrawCircleHole(false)
            setDrawValues(false)
        }

        chart.data = LineData(dataSet)
        chart.invalidate()  // Refresh chart
    }


    private fun setupWeeklyChart(events: List<MedicationEvent>) {
        val chart = binding.weeklyChart
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.legend.isEnabled = true

        // Get the current date and calculate the start of the current week (Monday)
        val today = LocalDate.now()
        val currentDayOfWeek = today.dayOfWeek.value // 1 (Monday) to 7 (Sunday)
        val startOfWeek = today.minusDays((currentDayOfWeek - 1).toLong())

        // Create a list of the 7 days in the current week
        val daysInWeek = (0..6).map { startOfWeek.plusDays(it.toLong()) }

        // Create labels for the x-axis
        val dayLabels = daysInWeek.map { date ->
            "${date.monthValue}/${date.dayOfMonth} ${date.dayOfWeek.toString().take(3)}"
        }

        // Group events by their actual date
        val eventsByDate = events.groupBy { event ->
            event.date.toLocalDate()
        }

        // Create entries for each day of the current week
        val entries = daysInWeek.mapIndexed { index, date ->
            val dayEvents = eventsByDate[date] ?: emptyList()
            BarEntry(index.toFloat(), dayEvents.size.toFloat())
        }

        val dataSet = BarDataSet(entries, "Events by Day").apply {
            color = Color.GREEN
            valueTextSize = 12f
            // Set the values' text color for the bars
            valueTextColor = if ((resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
        }

        // Highlight today's bar with a different color
        val todayIndex = currentDayOfWeek - 1
        if (todayIndex in entries.indices) {
            val colors = entries.mapIndexed { index, _ ->
                if (index == todayIndex) Color.BLUE else Color.GREEN
            }
            dataSet.colors = colors
        }

        // Detect dark mode and set label color accordingly.
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val labelColor = if (isDarkMode) Color.WHITE else Color.BLACK

        // Set colors for x-axis, y-axes, legend, and description
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
        xAxis.textColor = labelColor

        chart.axisLeft.textColor = labelColor
        chart.axisRight.textColor = labelColor
        chart.legend.textColor = labelColor
        chart.description.textColor = labelColor

        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
        chart.invalidate()
    }


    private fun setupDailyTimingChart(events: List<MedicationEvent>) {
        val chart = binding.dailyTimingChart
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.legend.isEnabled = true

        // Group events by hour
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

        // Detect dark mode and set label color accordingly.
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val labelColor = if (isDarkMode) Color.WHITE else Color.BLACK

        // Set up the data set for the chart.
        val dataSet = BarDataSet(entries, "Events by Time").apply {
            color = Color.CYAN
            valueTextSize = 12f
            valueTextColor = labelColor  // set value text color
        }

        // Configure x-axis
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(timeBlocks.keys.toList())
        xAxis.textColor = labelColor
        xAxis.labelRotationAngle = 45f

        // Set y-axis colors
        chart.axisLeft.textColor = labelColor
        chart.axisRight.textColor = labelColor

        // Set legend and description text color
        chart.legend.textColor = labelColor
        chart.description.textColor = labelColor

        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
        chart.invalidate()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}