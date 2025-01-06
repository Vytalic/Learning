package com.vytalitech.android.timekeeper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.vytalitech.android.timekeeper.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val categories: List<String>
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        // Inflate layout using binding class
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        // Use binding object to access views directly
        holder.binding.tvCategoryName.text = categories[position]
        holder.binding.btnStart.setOnClickListener {
            TODO("Add logic for starting the timer")
        }
        holder.binding.btnStop.setOnClickListener {
            TODO("Add logic for stopping the timer")
        }
    }

    override fun getItemCount(): Int = categories.size

    }
