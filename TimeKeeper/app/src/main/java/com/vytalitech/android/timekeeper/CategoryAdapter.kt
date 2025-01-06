package com.vytalitech.android.timekeeper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.vytalitech.android.timekeeper.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val categories: List<Category>,
    private val onStartClick: (Category) -> Unit,
    private val onStopClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        // Inflate layout using binding class
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        // Use binding object to access views directly
        val category = categories[position]
        holder.binding.tvCategoryName.text = category.name
        holder.binding.btnStart.setOnClickListener { onStartClick(category) }
        holder.binding.btnStop.setOnClickListener { onStopClick(category) }
    }

    override fun getItemCount(): Int = categories.size

    }
