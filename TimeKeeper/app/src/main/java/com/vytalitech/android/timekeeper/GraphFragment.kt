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

        val entries = categories.map { category ->
            PieEntry(category.totalTime.toFloat(), category.name)
        }

        val dataSet = PieDataSet(entries, "Category Time Distribution")
        dataSet.colors = getCustomColors() // Use custom colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.holeRadius = 0f
        pieChart.transparentCircleRadius = 0f

        // Disable the default legend
        pieChart.legend.isEnabled = false

        // Pass totalTime to the custom legend builder
        buildCustomLegend(categories, dataSet.colors, totalTime)

        pieChart.invalidate()
    }


    private fun buildCustomLegend(categories: List<Category>, colors: List<Int>, totalTime: Long) {
        val legendContainer = view?.findViewById<LinearLayout>(R.id.customLegend)
        legendContainer?.removeAllViews() // Clear any existing views

        // Calculate and sort categories by percentage in descending order
        val sortedCategories = categories.map { category ->
            val percentage = if (totalTime > 0) (category.totalTime * 100.0 / totalTime) else 0.0
            Pair(category, percentage)
        }.sortedByDescending { it.second }

        sortedCategories.forEachIndexed { index, (category, percentage) ->
            val legendItem = LayoutInflater.from(requireContext())
                .inflate(R.layout.legend_item, legendContainer, false)

            // Set the color indicator, cycling through colors if needed
            val colorView = legendItem.findViewById<View>(R.id.legendColor)
            val colorIndex = index % colors.size // Use modulo to cycle through colors
            colorView.setBackgroundColor(colors[colorIndex])

            // Set the category name and percentage
            val label = legendItem.findViewById<TextView>(R.id.legendLabel)
            label.text = "${category.name}: ${String.format("%.1f", percentage)}%"

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
