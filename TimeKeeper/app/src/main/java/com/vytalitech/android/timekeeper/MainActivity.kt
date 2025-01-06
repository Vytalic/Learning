package com.vytalitech.android.timekeeper

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vytalitech.android.timekeeper.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: CategoryAdapter
    private val activeTimers = mutableMapOf<Int, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using binding class
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = DatabaseProvider.getDatabase(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // Add Category FAB
        binding.fabAddCategory.setOnClickListener {
            // Create AlertDialog to get user input
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Add new category")

            // Set up the input field
            val input = android.widget.EditText(this)
            input.hint = "Enter category name"
            builder.setView(input)

            builder.setPositiveButton("Add") {_, _ ->
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

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

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
                    Toast.makeText(this@MainActivity,
                        "Removed: ${lastCategory.name}",
                        Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(this@MainActivity,
                        "No categories to delete",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        // Refresh categories when activity starts
        lifecycleScope.launch {
            refreshCategories()
        }
    }



    private suspend fun refreshCategories() {
        val categories = database.categoryDao().getAllCategories()

        adapter = CategoryAdapter(categories, onStartClick = { category ->
            if (activeTimers[category.id] == true) {
                Toast.makeText(this, "${category.name} is already running!", Toast.LENGTH_SHORT)
                    .show()
                return@CategoryAdapter
            }
            activeTimers[category.id] = true

            lifecycleScope.launch {
                while (activeTimers[category.id] == true) {
                    category.totalTime += 1
                    database.categoryDao().updateCategory(category)
                    refreshCategories()
                    delay(1000)
                }
            }
        }, onStopClick = { category ->
            activeTimers[category.id] = false
        })
        binding.recyclerView.adapter = adapter
    }


}

