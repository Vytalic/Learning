package com.vytalitech.android.timekeeper

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.vytalitech.android.timekeeper.databinding.ItemCategoryBinding
import java.util.Collections
import java.util.Locale

class CategoryAdapter(
    val categories: MutableList<Category>,
    private val categoryTimes: MutableMap<Int, Long>, // Add categoryTimes as a parameter
    private var activeTimers: Map<Int, Boolean> = emptyMap(),
    private val onStartClick: (Category) -> Unit,
    private val onStopClick: (Category) -> Unit,
    private val onRemoveClick: (Category) -> Unit,
    private val onRemoveModeUpdate: (Boolean) -> Unit // Notify parent when mode changes
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    private var isRemoveModeActive = false
    private var currentlyDraggingPosition: Int? = null


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
        val isRunning = activeTimers[category.id] == true

        Log.d("CategoryAdapter", "Binding item at position $position, categoryId=${category.id}, isRunning=$isRunning")

        holder.binding.tvCategoryName.text = category.name
        holder.binding.tvTimeTracker.text = formatTime(categoryTimes[category.id] ?: 0)

        // Invert colors if this is the currently dragged item
        if (position == currentlyDraggingPosition) {
            holder.binding.root.setBackgroundColor(holder.itemView.context.getColor(R.color.invertedBackground))
            holder.binding.tvCategoryName.setTextColor(holder.itemView.context.getColor(R.color.invertedText))
        } else {
            // Reset to normal colors
            holder.binding.root.setBackgroundColor(holder.itemView.context.getColor(R.color.normalBackground))
            holder.binding.tvCategoryName.setTextColor(holder.itemView.context.getColor(R.color.normalText))
        }

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
            holder.binding.btnToggle.apply {
                text = if (isRunning) {
                    setBackgroundColor(context.getColor(R.color.btnOrange))
                    context.getString(R.string.btn_stop)
                } else {
                    setBackgroundColor(context.getColor(R.color.btnGreen))
                    context.getString(R.string.btn_start)
                }

                setOnClickListener {
                    Log.d("CategoryAdapter - setonclicklistener", "CategoryId=${category.id}, isRunning=$isRunning")
                    if (isRunning) {
                        onStopClick(category)
                    } else {
                        onStartClick(category)
                    }
                }
            }
        }
    }


    fun updateActiveTimers(newActiveTimers: Map<Int, Boolean>) {
        activeTimers = newActiveTimers
        notifyDataSetChanged() // Refresh the entire list
    }

    fun getCurrentOrder(): List<Category> {
        return categories.toList() // Return a copy of the current list
    }


    fun enableReorderMode(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // Allow dragging in vertical direction
                return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition

                // Swap items and notify adapter
                Collections.swap(categories, fromPosition, toPosition)
                notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe action needed
            }

            override fun isLongPressDragEnabled(): Boolean {
                return false // Disable long-press drag
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return false // Disable swipe
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f // Add visual feedback
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f // Reset visual feedback
            }
        })

        // Attach the ItemTouchHelper to the RecyclerView
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Intercept touch events to force dragging immediately
        recyclerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val childView = recyclerView.findChildViewUnder(event.x, event.y)
                if (childView != null) {
                    val viewHolder = recyclerView.getChildViewHolder(childView)
                    itemTouchHelper.startDrag(viewHolder) // Start dragging immediately
                }
            }
            false // Allow further handling of the event
        }
    }

    fun disableReorderMode(recyclerView: RecyclerView) {
        // Remove the touch listener to stop handling reorder-related events
        recyclerView.setOnTouchListener(null)

        // Attach a null ItemTouchHelper to detach the current one
        ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return 0 // Disable all movements
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No action
            }
        }).attachToRecyclerView(recyclerView)
    }









    fun updateTimes(times: Map<Int, Long>) {
        times.forEach { (id, newTime) ->
            if (categoryTimes[id] != newTime) {
                categoryTimes[id] = newTime
                val position = categories.indexOfFirst { it.id == id }
                if (position != -1) {
                    notifyItemChanged(position, newTime) // Use payloads
                }
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<Category>) {
        // Sort the categories by their order before updating the adapter
        val sortedCategories = newCategories.sortedBy { it.order }

        // Calculate differences between the old and new lists
        val diffCallback = CategoryDiffCallback(categories, sortedCategories)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Update the internal list and notify the adapter
        categories.clear()
        categories.addAll(sortedCategories)
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


