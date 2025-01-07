package com.vytalitech.android.timekeeper

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.Calendar


class GraphFragment : Fragment(R.layout.fragment_graph) {
    private var startDate: String? = null
    private var endDate: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnStartDate = view.findViewById<Button>(R.id.btnSelectStartDate)
        val btnEndDate = view.findViewById<Button>(R.id.btnSelectEndDate)
        val lineChart = view.findViewById<LineChart>(R.id.lineChart)

        val calendar = Calendar.getInstance()

        // Start date picker
        btnStartDate.setOnClickListener {
            val startDatePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    startDate = "$year-${month + 1}-$day"
                    Toast.makeText(requireContext(), "Start Date: $startDate", Toast.LENGTH_SHORT)
                        .show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            startDatePicker.show()
        }

        // End date picker
        btnEndDate.setOnClickListener {
            val endDatePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    endDate = "$year-${month + 1}-$day"
                    Toast.makeText(requireContext(), "End Date: $endDate", Toast.LENGTH_SHORT).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            endDatePicker.show()
        }

        // Load graph after setting both dates
        if (startDate != null && endDate != null) {
            loadGraphData(lineChart)
        }

        // Data
        val dataSet = LineDataSet(getSampleEntries(), "Category Time")
        val lineData = LineData(dataSet)

        lineChart.data = lineData
        lineChart.invalidate()
    }

    private fun getSampleEntries(): List<Entry> {
        return listOf(
            Entry(1f, 100f),
            Entry(2f, 150f),
            Entry(3f, 200f)
        )
    }

    private fun loadGraphData(lineChart: LineChart) {
        val dataSet = LineDataSet(getSampleEntries(), "Category Time")
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }

}