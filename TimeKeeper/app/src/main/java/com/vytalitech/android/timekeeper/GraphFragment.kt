package com.vytalitech.android.timekeeper

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch
import java.util.Locale

class GraphFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var database: AppDatabase
    private lateinit var viewModel: TimerViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_graph, container, false)

        // Initialize the database
        database = DatabaseProvider.getDatabase(requireContext())

        // Initialize PieChart
        pieChart = view.findViewById(R.id.pieChart)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[TimerViewModel::class.java]

        // Observe changes to categories and update the pie chart
        viewModel.categoryList.observe(viewLifecycleOwner) { categories ->
            lifecycleScope.launch {
                displayCategoryData(categories) // Pass the List<Category> to displayCategoryData
            }
        }
    }

    private fun displayCategoryData(categories: List<Category>) {
        val totalTime = categories.sumOf { it.totalTime }

        // Sort categories by total time in descending order
        val sortedCategories = categories.sortedByDescending { it.totalTime }

        val entries = sortedCategories.map { category ->
            PieEntry(category.totalTime.toFloat(), category.name)
        }

        val colors = getCustomColors()

        // Map categories to colors
        val categoryColors = sortedCategories.mapIndexed { index, category ->
            category to colors[index % colors.size]
        }.toMap()

        val dataSet = PieDataSet(entries, "Category Time Distribution")
        dataSet.colors = sortedCategories.map { categoryColors[it] ?: Color.BLACK }
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)

        // Set a custom ValueFormatter for the PieData to remove decimals
        data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString() // Format as integer
            }
        })
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.holeRadius = 0f
        pieChart.transparentCircleRadius = 0f
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.legend.isEnabled = false

        buildCustomLegend(sortedCategories, categoryColors, totalTime)

        pieChart.invalidate()
    }


    private fun buildCustomLegend(
        categories: List<Category>,
        categoryColors: Map<Category, Int>,
        totalTime: Long
    ) {
        val legendContainer = view?.findViewById<LinearLayout>(R.id.customLegend)
        legendContainer?.removeAllViews()

        categories.forEach { category ->
            val percentage = if (totalTime > 0) (category.totalTime * 100.0 / totalTime) else 0.0
            val legendItem = LayoutInflater.from(requireContext())
                .inflate(R.layout.legend_item, legendContainer, false)

            val colorView = legendItem.findViewById<View>(R.id.legendColor)
            colorView.setBackgroundColor(categoryColors[category] ?: Color.BLACK)

            val label = legendItem.findViewById<TextView>(R.id.legendLabel)
            label.text = getString(
                R.string.legend_label_text,
                category.name,
                String.format(Locale.US, "%.1f", percentage)
            )

            legendContainer?.addView(legendItem)
        }
    }

    private fun getCustomColors(): List<Int> {
        return listOf(
            Color.parseColor("#FF5733"), // Red
            Color.parseColor("#33FF57"), // Green
            Color.parseColor("#5733FF"), // Blue
            Color.parseColor("#FFD133"), // Yellow
            Color.parseColor("#33FFF5"), // Cyan
            Color.parseColor("#FF8C33"), // Orange
            Color.parseColor("#A533FF"), // Purple
            Color.parseColor("#FF33A5"), // Pink
            Color.parseColor("#33FF8C"), // Light Green
            Color.parseColor("#FF3333"), // Bright Red
            Color.parseColor("#33A5FF"), // Light Blue
            Color.parseColor("#F533FF"), // Magenta
            Color.parseColor("#FFC933"), // Light Orange
            Color.parseColor("#33FFF8"), // Light Cyan
            Color.parseColor("#8C33FF"), // Violet
            Color.parseColor("#33FF33")  // Bright Green
        )
    }


}
