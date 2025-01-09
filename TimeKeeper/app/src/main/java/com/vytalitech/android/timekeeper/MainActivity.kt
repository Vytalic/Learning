package com.vytalitech.android.timekeeper

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
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

    private var isServiceBound = false
    private var timerService: TimerService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? TimerService.TimerBinder
            timerService = binder?.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val savedCategoryId = savedInstanceState?.getInt("CURRENT_CATEGORY_ID", -1) ?: -1
        val savedElapsedTime = savedInstanceState?.getLong("CURRENT_ELAPSED_TIME", 0L) ?: 0L

        if (savedCategoryId != -1) {
            currentRunningCategoryId = savedCategoryId
            timerViewModel.updateTimerState(savedCategoryId, savedElapsedTime)
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

        // Initialize the database
        database = DatabaseProvider.getDatabase(this)

        // Initialize TimerViewModel
        val factory = TimerViewModelFactory(database)
        timerViewModel = ViewModelProvider(this, factory)[TimerViewModel::class.java]


        // Initialize adapter with default values
        adapter = CategoryAdapter(
            categories = mutableListOf(), // An empty mutable list for categories
            categoryTimes = emptyMap(),   // An empty map for category times
            activeTimers = emptyMap(),    // An empty map for active timers
            onStartClick = { _ -> },      // A no-op lambda for onStartClick
            onStopClick = { _ -> },       // A no-op lambda for onStopClick
            onRemoveClick = { _ -> },     // A no-op lambda for onRemoveClick
            onRemoveModeUpdate = { _ -> } // A no-op lambda for onRemoveModeUpdate
        )

        lifecycleScope.launch {
            loadCategories()
        }

        timerViewModel.timerEvent.observe(this) { event ->
            event?.let {
                if (it.isRunning) {
                    // Check if a different timer is running
                    if (currentRunningCategoryId != it.categoryId) {
                        startTimerService(it.categoryId, it.elapsedTime)
                        currentRunningCategoryId = it.categoryId
                    } else {
                        Log.d("MainActivity", "TimerService already running for categoryId: ${it.categoryId}")
                    }
                } else {
                    // Stop the service only if the currently running timer matches
                    if (currentRunningCategoryId == it.categoryId) {
                        stopTimerService()
                        currentRunningCategoryId = null
                    }
                }
            }
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

        // Trigger refresh when activity is recreated
        timerViewModel.reEmitCategoryTimes()

        lifecycleScope.launch {
            refreshCategories()
        }

    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("CURRENT_CATEGORY_ID", currentRunningCategoryId ?: -1)
        outState.putLong("CURRENT_ELAPSED_TIME", timerService?.elapsedTime?.get() ?: 0L)
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

    private var isServiceRunning = false


    override fun onStop() {
        super.onStop()

        // Unbind the service
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Simulate "Stop Timer" when app is closed
        currentRunningCategoryId?.let { categoryId ->
            timerViewModel.stopTimer(categoryId)
            stopTimerService()
            Log.d("MainActivity", "App closed. Timer stopped for categoryId: $categoryId.")
        }
    }

    private fun startTimerService(categoryId: Int, elapsedTime: Long) {
        Log.d("MainActivity", "startTimerService called with categoryId: $categoryId, elapsedTime: $elapsedTime")
        val categoryName = getCategoryNameById(categoryId)
        if (categoryName == "Unknown") {
            Log.e("MainActivity", "Category name not found for categoryId: $categoryId")
            return
        }


        if (!isServiceRunning) {
            val intent = Intent(this, TimerService::class.java).apply {
                putExtra("CATEGORY_ID", categoryId)
                putExtra("CATEGORY_NAME", categoryName)
                putExtra("ELAPSED_TIME", elapsedTime)
            }
            Log.d("MainActivity", "Starting TimerService with categoryName: $categoryName, elapsedTime: $elapsedTime")
            startService(intent)
            isServiceRunning = true
        } else {
            Log.d("MainActivity", "TimerService already running for categoryId: $categoryId")
        }
    }

    private fun stopTimerService() {
        if (isServiceRunning) {
            Log.d("MainActivity", "Stopping TimerService")
            stopService(Intent(this, TimerService::class.java))
            isServiceRunning = false
        } else {
            Log.d("MainActivity", "TimerService is not running.")
        }
    }

    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, findViewById(R.id.action_menu))
        popupMenu.menuInflater.inflate(R.menu.fab_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_add -> handleAddCategory()
                R.id.action_remove -> homeFragment?.activateRemoveMode()
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
                    timerViewModel.refreshCategories() // Notify ViewModel to refresh categories
                }
            } else {
                Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        // Show the dialog
        builder.show()
    }

    private suspend fun loadCategories() {
        val categories = database.categoryDao().getAllCategories()
        categoryMap.clear()
        categoryMap.putAll(categories.associate { it.id to it.name })
        Log.d("MainActivity", "Preloaded categories: $categoryMap")
    }

    private fun getCategoryNameById(categoryId: Int): String {
        return categoryMap[categoryId] ?: "Unknown"
    }



    private suspend fun refreshCategories() {
        val categories = database.categoryDao().getAllCategories()

        // Update adapter's data directly
        adapter.updateCategories(categories)

        // Load persisted totalTime into categoryTimes
        val timesMap = categories.associate { it.id to it.totalTime }
        timerViewModel.categoryTimes.value = timesMap.toMutableMap()
    }
    private var homeFragment: HomeFragment? = null

//    private fun showHomeFragment() {
//        val fragment = HomeFragment()
//        homeFragment = fragment // Keep a reference to the fragment
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.fragmentContainer, fragment)
//            .commit()
//    }

    private fun showHomeFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        val fragment = supportFragmentManager.findFragmentByTag("HomeFragment")
        if (fragment == null) {
            val homeFragment = HomeFragment()
            transaction.add(R.id.fragmentContainer, homeFragment, "HomeFragment")
        }
        supportFragmentManager.fragments.forEach {
            if (it is HomeFragment) {
                transaction.show(it)
            } else {
                transaction.hide(it)
            }
        }
        transaction.commit()
    }

    private fun showGraphFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        val fragment = supportFragmentManager.findFragmentByTag("GraphFragment")
        if (fragment == null) {
            val graphFragment = GraphFragment()
            transaction.add(R.id.fragmentContainer, graphFragment, "GraphFragment")
        }
        supportFragmentManager.fragments.forEach {
            if (it is GraphFragment) {
                transaction.show(it)
            } else {
                transaction.hide(it)
            }
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

