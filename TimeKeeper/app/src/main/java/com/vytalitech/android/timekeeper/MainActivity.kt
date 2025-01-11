package com.vytalitech.android.timekeeper

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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

    private val categoryMap = mutableMapOf<Int, String>()
    private var currentRunningCategoryId: Int? = null

    override fun onStop() {
        super.onStop()
        saveAllRunningTimers()


    }

    private fun saveAllRunningTimers() {
        val activeTimers = timerViewModel.activeTimers.value ?: return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        lifecycleScope.launch {
            activeTimers.forEach { (categoryId, isRunning) ->
                if (isRunning) {
                    val currentTime = System.currentTimeMillis()

                    // Retrieve the category from the database
                    val category = database.categoryDao().getCategoryById(categoryId)
                    if (category?.startTime != null) {
                        // Calculate elapsed time
                        val elapsedTime = (currentTime - category.startTime) / 1000L // Convert ms to seconds

                        // Update the category's total time
                        val updatedCategory = category.copy(
                            totalTime = category.totalTime + elapsedTime,
                            startTime = null // Clear the start time since the timer is being saved
                        )
                        database.categoryDao().updateCategory(updatedCategory)

                        // Cancel the notification for this timer
                        notificationManager.cancel(categoryId) // Assumes categoryId is used as notification ID

                        // Log for debugging
                        Log.d("MainActivity", "Saved timer for category $categoryId to database: $elapsedTime seconds")
                    } else {
                        Log.w("MainActivity", "No valid startTime found for category $categoryId")
                    }
                }
            }
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started")

        database = DatabaseProvider.getDatabase(this)

        database.categoryDao().getAllCategories().observe(this) { categories ->
            adapter.updateCategories(categories) // Update adapter when categories change
        }

        val savedCategoryId = savedInstanceState?.getInt("CURRENT_CATEGORY_ID", -1) ?: -1

        if (savedCategoryId != -1) {
            currentRunningCategoryId = savedCategoryId

        }

        // Check and request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission granted")
            } else {
                Log.e("MainActivity", "POST_NOTIFICATIONS permission not granted, requesting now")
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Inflate layout using binding class
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find and set up the custom Toolbar
        setSupportActionBar(binding.toolbar)


        // Initialize TimerViewModel
        val factory = TimerViewModelFactory(application, database) // Pass the database instance here
        timerViewModel = ViewModelProvider(this, factory)[TimerViewModel::class.java]




        // Initialize adapter with default values
        adapter = CategoryAdapter(
            categories = mutableListOf(), // An empty mutable list for categories
            categoryTimes = mutableMapOf(),   // An empty map for category times
            activeTimers = emptyMap(),    // An empty map for active timers
            onStartClick = { _ -> },      // A no-op lambda for onStartClick
            onStopClick = { _ -> },       // A no-op lambda for onStopClick
            onRemoveClick = { _ -> },     // A no-op lambda for onRemoveClick
            onRemoveModeUpdate = { _ -> } // A no-op lambda for onRemoveModeUpdate
        )



        lifecycleScope.launch {
            loadCategories()
        }


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

        // Observe timer states
        timerViewModel.categoryTimes.observe(this) { times ->
            adapter.updateTimes(times)
        }

        lifecycleScope.launch {
            refreshCategories()
        }

        Log.d("MainActivity", "onCreate completed")

    }




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission granted by user")
            } else {
                Log.e("MainActivity", "POST_NOTIFICATIONS permission denied by user")
                Toast.makeText(this, "Notification permission is required to display timers", Toast.LENGTH_LONG).show()
            }
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
        val popupMenu = PopupMenu(this, this.findViewById(R.id.action_menu))
        popupMenu.menuInflater.inflate(R.menu.fab_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_add -> handleAddCategory()
                R.id.action_remove -> {
                    val homeFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? HomeFragment
                    homeFragment?.activateRemoveMode()
                }
                R.id.action_reorder -> {
                    val homeFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? HomeFragment
                    homeFragment?.enterReorderMode() // Trigger reorder mode
                }
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
            val time = null
            if (categoryName.isNotEmpty()) {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date()
                    )
                lifecycleScope.launch {
                    // Get the maximum order value from the existing categories
                    val maxOrder = database.categoryDao().getMaxOrder() ?: 0
                    val newCategory = Category(
                        name = categoryName,
                        startTime = time,
                        date = currentDate,
                        order = maxOrder + 1 // Set the new category order
                    )

                    // Insert category in coroutine after confirmation
                    database.categoryDao().insertCategory(newCategory)
                }
            } else {
                Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        // Show the dialog
        builder.show()
    }


    private fun loadCategories() {
        database.categoryDao().getAllCategories().observe(this) { categories ->
            categoryMap.clear()
            categoryMap.putAll(categories.associate { it.id to it.name })
            Log.d("MainActivity", "Preloaded categories: $categoryMap")
        }
    }

    private fun refreshCategories() {
        database.categoryDao().getAllCategories().observe(this) { categories ->
            adapter.updateCategories(categories) // Update adapter with new categories
        }
    }


    private fun showHomeFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        val homeFragment = supportFragmentManager.findFragmentByTag("HomeFragment")
        val graphFragment = supportFragmentManager.findFragmentByTag("GraphFragment")

        if (homeFragment == null) {
            transaction.replace(R.id.fragmentContainer, HomeFragment(), "HomeFragment")
        } else {
            transaction.show(homeFragment)
        }

        graphFragment?.let {
            transaction.hide(it)
        }

        transaction.commit()
    }




    private fun showGraphFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        val graphFragment = supportFragmentManager.findFragmentByTag("GraphFragment")
        val homeFragment = supportFragmentManager.findFragmentByTag("HomeFragment")

        if (graphFragment == null) {
            transaction.replace(R.id.fragmentContainer, GraphFragment(), "GraphFragment")
        } else {
            transaction.show(graphFragment)
        }

        homeFragment?.let {
            transaction.hide(it)
        }

        transaction.commit()
    }

    private fun showSettingsFragment() {
        Toast.makeText(this@MainActivity, "Feature coming soon!", Toast.LENGTH_SHORT).show()
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.fragmentContainer, SettingsFragment())
//            .commit()
    }

}

