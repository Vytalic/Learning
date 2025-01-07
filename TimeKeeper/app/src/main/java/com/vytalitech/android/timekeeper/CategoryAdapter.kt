package com.vytalitech.android.timekeeper

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vytalitech.android.timekeeper.databinding.ItemCategoryBinding
import java.util.Locale

class CategoryAdapter(
    private val categories: MutableList<Category>,
    private val onStartClick: (Category) -> Unit,
    private val onStopClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    //private val activeTimers = mutableMapOf<Int, Boolean>()
    private val categoryTimes = mutableMapOf<Int, Long>()

    class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        // Inflate layout using binding class
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        // Use binding object to access views directly
        val category = categories[position]

        // Set category name and initial time
        holder.binding.tvCategoryName.text = category.name
        holder.binding.tvTimeTracker.text = formatTime(categoryTimes[category.id] ?: 0)

        holder.binding.btnStart.setOnClickListener { onStartClick(category) }
        holder.binding.btnStop.setOnClickListener { onStopClick(category) }
    }

    fun updateTimes(times: Map<Int, Long>) {
        categoryTimes.clear()
        categoryTimes.putAll(times)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<Category>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }

    // Helper function: formats time in HH:mm:ss
    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, secs)

    }
}


