package com.vytalitech.android.timekeeper

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vytalitech.android.timekeeper.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: CategoryAdapter
    private lateinit var timerViewModel: TimerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using binding class
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find and set up the custom Toolbar
        setSupportActionBar(binding.toolbar)

        // Initialize the database
        database = DatabaseProvider.getDatabase(this)

        // Initialize the TimerViewModel with the factory
        val factory = TimerViewModelFactory(database)
        timerViewModel = ViewModelProvider(this, factory).get(TimerViewModel::class.java)



        // Set up the BottomNavigationView
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showHomeFragment()
                    true
                }
                R.id.nav_graph -> {
                    showGraphFragment()
                    true
                }
                R.id.nav_settings -> {
                    showSettingsFragment()
                    true
                }
                else -> false
            }
        }

        // Load the default fragment
        if (savedInstanceState == null) {
            showHomeFragment()
        }

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

        // Trigger refresh when activity is recreated
        timerViewModel.reEmitCategoryTimes()

        lifecycleScope.launch {
            refreshCategories()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_menu -> {
                // Show popupmenu or handle click
                showPopupMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, findViewById(R.id.action_menu))
        popupMenu.menuInflater.inflate(R.menu.fab_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_add -> handleAddCategory()
                R.id.action_remove -> handleRemoveCategory()
            }
            true
        }
        popupMenu.show()
    }

    private fun handleAddCategory() {
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
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date()
                    )
                val newCategory = Category(name = categoryName, date = currentDate)

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

    private fun handleRemoveCategory() {
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

    private suspend fun refreshCategories() {
        val categories = database.categoryDao().getAllCategories()

        // Update adapter's data directly
        adapter.updateCategories(categories)

        // Load persisted totalTime into categoryTimes
        val timesMap = categories.associate { it.id to it.totalTime }
        timerViewModel.categoryTimes.value = timesMap.toMutableMap()
    }

    private fun showHomeFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()
    }

    private fun showGraphFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, GraphFragment())
            .commit()
    }

    private fun showSettingsFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SettingsFragment())
            .commit()
    }

}

