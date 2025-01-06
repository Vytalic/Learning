package com.vytalitech.android.timekeeper

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vytalitech.android.timekeeper.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: CategoryAdapter
    private lateinit var timerViewModel: TimerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the database
        database = DatabaseProvider.getDatabase(this)

        // Initialize the TimerViewModel with the factory
        val factory = TimerViewModelFactory(database)
        timerViewModel = ViewModelProvider(this, factory).get(TimerViewModel::class.java)

        // Inflate layout using binding class
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with an empty list
        adapter = CategoryAdapter(
            mutableListOf(),
            onStartClick = { category -> timerViewModel.startTimer(category.id) },
            onStopClick = { category -> timerViewModel.stopTimer(category.id) }
        )
        binding.recyclerView.adapter = adapter

        // Observe timer states
        timerViewModel.categoryTimes.observe(this) { times ->
            adapter.updateTimes(times)
        }

        // Add Category FAB
        binding.fabAddCategory.setOnClickListener {
            // Create AlertDialog to get user input
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Add new category")

            // Set up the input field
            val input = android.widget.EditText(this)
            input.hint = "Enter category name"
            builder.setView(input)

            builder.setPositiveButton("Add") { _, _ ->
                val categoryName = input.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    val newCategory = Category(name = categoryName)

                    // Insert category in coroutine after confirmation
                    lifecycleScope.launch {
                        database.categoryDao().insertCategory(newCategory)
                        refreshCategories()
                    }
                } else {
                    Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }

            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            // Show the dialog
            builder.show()
        }

        // Remove Category FAB
        binding.fabRemoveCategory.setOnClickListener {
            lifecycleScope.launch {
                val categories = database.categoryDao().getAllCategories()
                if (categories.isNotEmpty()) {
                    val lastCategory = categories.last()
                    database.categoryDao().deleteCategory(lastCategory)
                    refreshCategories()
                    Toast.makeText(
                        this@MainActivity,
                        "Removed: ${lastCategory.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "No categories to delete",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Trigger refresh when activity is recreated
        timerViewModel.reEmitCategoryTimes()

        lifecycleScope.launch {
            refreshCategories()
        }
    }

    private suspend fun refreshCategories() {
        val categories = database.categoryDao().getAllCategories()

        // Update adapter's data directly
        adapter.updateCategories(categories)

        // Load persisted totalTime into categoryTimes
        val timesMap = categories.associate { it.id to it.totalTime }
        timerViewModel.categoryTimes.value = timesMap.toMutableMap()

//        adapter = CategoryAdapter(
//            categories,
//            onStartClick = { category -> timerViewModel.startTimer(category.id) },
//            onStopClick = { category -> timerViewModel.stopTimer(category.id) }
//        )
//        binding.recyclerView.adapter = adapter
    }
}

