package com.vytalitech.android.timekeeper

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class TimerViewModel(application: Application, private val database: AppDatabase) : AndroidViewModel(application) {

    private val categoryDao = DatabaseProvider.getDatabase(application).categoryDao()
    val categoryList: LiveData<List<Category>> = categoryDao.getAllCategories()


    // Used new
    private val _categoryTimes = MutableLiveData<MutableMap<Int, Long>>(mutableMapOf())
    val categoryTimes: LiveData<MutableMap<Int, Long>> get() = _categoryTimes

    private val _activeTimers = MutableLiveData<Map<Int, Boolean>>(mutableMapOf())
    val activeTimers: LiveData<Map<Int, Boolean>> get() = _activeTimers

    private var timerJob: Job? = null
    val isRemoveMode = MutableLiveData<Boolean>().apply { value = false }



    // LiveData for observing categories
    //val categoryList: LiveData<List<Category>> = categoryDao.getAllCategories()


    fun startUpdatingTimers() {
        // Check if a timer job is already active
        if (timerJob?.isActive == true) {
            return // Do nothing if a timer job is already running
        }

        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000) // Update every second
                val updatedTimes = _categoryTimes.value?.toMutableMap() ?: mutableMapOf()
                _activeTimers.value?.forEach { (categoryId, isRunning) ->
                    if (isRunning) {
                        updatedTimes[categoryId] = (updatedTimes[categoryId] ?: 0) + 1
                    }
                }
                _categoryTimes.postValue(updatedTimes)
            }
        }
    }

    fun loadInitialTimes(categories: List<Category>) {
        val currentActiveTimers = _activeTimers.value ?: emptyMap()
        val updatedTimes = _categoryTimes.value?.toMutableMap() ?: mutableMapOf()

        categories.forEach { category ->
            if (currentActiveTimers[category.id] != true) {
                // Initialize only if the timer is not running
                updatedTimes[category.id] = category.totalTime
            }
        }

        _categoryTimes.value = updatedTimes
    }


//    fun stopUpdatingTimers() {
//        timerJob?.cancel()
//        timerJob = null
//    }

    fun startTimer(categoryId: Int, categoryName: String, context: Context) {
        _activeTimers.value = _activeTimers.value?.toMutableMap()?.apply {
            if (this[categoryId] == true) {
                // Timer is already running for this category; do nothing
                Log.d("TimerViewModel", "Timer already running for category $categoryId")
                return
            }
            this[categoryId] = true
        }

        val startTime = System.currentTimeMillis()
        viewModelScope.launch {
            val category = database.categoryDao().getCategoryById(categoryId)
            if (category != null) {
                val updatedCategory = category.copy(startTime = startTime)
                database.categoryDao().updateCategory(updatedCategory)
            }
        }


        _activeTimers.value = _activeTimers.value // Trigger LiveData observer
        Log.d("TimerViewModel", "Started timer for category $categoryId")
        TimerService.startService(context, categoryId, categoryName)
    }


    fun stopTimer(categoryId: Int, context: Context) {
        if (_activeTimers.value?.get(categoryId) == false) return

        // Mark the timer as inactive
        _activeTimers.value = _activeTimers.value?.toMutableMap()?.apply {
            this[categoryId] = false
        }

        // Trigger LiveData observer
        _activeTimers.value = _activeTimers.value

        // Save the current elapsed time to the database
        viewModelScope.launch {
            val category = database.categoryDao().getCategoryById(categoryId)
            if (category?.startTime != null) {
                val elapsedTime = (System.currentTimeMillis() - category.startTime) / 1000L

                val updatedCategory = category.copy(
                    totalTime = category.totalTime + elapsedTime,
                    startTime = null // Clear the start time
                )
                database.categoryDao().updateCategory(updatedCategory)
            } else {
                Log.w("TimerViewModel", "No start time found for category $categoryId")
            }
        }

        // Stop the TimerService
        Log.d("TimerViewModel", "Stopped timer for category $categoryId")
        val intent = Intent(context, TimerService::class.java)
        context.stopService(intent)
    }
}