package com.vytalitech.android.timekeeper

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.vytalitech.android.timekeeper.databinding.ItemCategoryBinding
import java.util.Locale

class CategoryAdapter(
    private val categories: MutableList<Category>,
    private val categoryTimes: Map<Int, Long>, // Add categoryTimes as a parameter
    private val activeTimers: Map<Int, Boolean>,
    private val onStartClick: (Category) -> Unit,
    private val onStopClick: (Category) -> Unit,
    private val onRemoveClick: (Category) -> Unit,
    private val onRemoveModeUpdate: (Boolean) -> Unit // Notify parent when mode changes
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    private val mutableCategoryTimes = categoryTimes.toMutableMap() // Create a mutable copy
    private var isRemoveModeActive = false

    fun enableRemoveMode() {
        isRemoveModeActive = true
        notifyItemRangeChanged(0, itemCount)
        onRemoveModeUpdate(true) // Notify parent

    }

    fun disableRemoveMode() {
        isRemoveModeActive = false
        notifyItemRangeChanged(0, itemCount)
        onRemoveModeUpdate(false) // Notify parent

    }




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        // Inflate layout using binding class
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        holder.binding.tvCategoryName.text = category.name
        holder.binding.tvTimeTracker.text = formatTime(mutableCategoryTimes[category.id] ?: 0)

        if (isRemoveModeActive) {
            holder.binding.btnToggle.apply {
                text = context.getString(R.string.btn_remove)
                setBackgroundColor(context.getColor(R.color.btnRed))
                setOnClickListener {
                    AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.delete_category_title))
                        .setMessage(context.getString(R.string.delete_category_message, category.name))
                        .setPositiveButton(context.getString(R.string.yes)) { _, _ ->
                            onRemoveClick(category)
                            if (categories.size == 1) disableRemoveMode() // Auto-exit if last category
                        }
                        .setNegativeButton(context.getString(R.string.no), null)
                        .show()
                }
            }
        } else {
            val isRunning = activeTimers[category.id] == true
            holder.binding.btnToggle.apply {
                text = if (isRunning) {
                    setBackgroundColor(context.getColor(R.color.btnOrange))
                    context.getString(R.string.btn_stop)
                } else {
                    setBackgroundColor(context.getColor(R.color.btnGreen))
                    context.getString(R.string.btn_start)
                }

                setOnClickListener {
                    if (isRunning) {
                        onStopClick(category)
                    } else {
                        onStartClick(category)
                    }
                }
            }
        }
    }


    fun updateTimes(times: Map<Int, Long>) {
        times.forEach { (id, newTime) ->
            if (mutableCategoryTimes[id] != newTime) {
                mutableCategoryTimes[id] = newTime
                val position = categories.indexOfFirst { it.id == id }
                if (position != -1) {
                    notifyItemChanged(position, newTime) // Use payloads
                }
            }
        }
    }



    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<Category>) {
        val diffCallback = CategoryDiffCallback(categories, newCategories)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        categories.clear()
        categories.addAll(newCategories)
        diffResult.dispatchUpdatesTo(this) // Notify only the changed items
    }


    // Helper function: formats time in HH:mm:ss
    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.ROOT, "%03d:%02d:%02d", hours, minutes, secs)

    }
}


