package com.vytalitech.android.timekeeper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vytalitech.android.timekeeper.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using binding class
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = DatabaseProvider.getDatabase(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.fabAddCategory.setOnClickListener {
            val newCategoryName = "New Category ${System.currentTimeMillis() % 1000}"
            val newCategory = Category(name = newCategoryName)

            // Insert category in a coroutine
            lifecycleScope.launch {
                database.categoryDao().insertCategory(newCategory)
                refreshCategories()
            }
        }


        // Load categories from database
        lifecycleScope.launch {
            val categories = database.categoryDao().getAllCategories()
            adapter = CategoryAdapter(categories.map { it.name }) // Maps to list of names
            binding.recyclerView.adapter = adapter
        }


//        // Set up RecyclerView with adapter
//        val categories = mutableListOf("Category 1", "Category 2")
//        val adapter = CategoryAdapter(categories)
//        binding.recyclerView.layoutManager = LinearLayoutManager(this)
//        binding.recyclerView.adapter = adapter




    }

    private suspend fun refreshCategories() {
        val categories = database.categoryDao().getAllCategories()
        adapter = CategoryAdapter(categories.map { it.name })
        binding.recyclerView.adapter = adapter
    }
}