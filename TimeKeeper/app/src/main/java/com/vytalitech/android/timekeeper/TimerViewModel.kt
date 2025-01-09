package com.vytalitech.android.timekeeper

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TimerEvent(val categoryId: Int, val elapsedTime: Long, val isRunning: Boolean)

class TimerViewModel(private val database: AppDatabase) : ViewModel() {
    val timerEvent = MutableLiveData<TimerEvent?>()
    private val _categoryList = MutableLiveData<List<Category>>()
    val categoryList: MutableLiveData<List<Category>> get() = _categoryList
    val categories = MutableLiveData<List<Category>>()


    fun refreshCategories() {
        viewModelScope.launch {
            val updatedCategories = database.categoryDao().getAllCategories()
            Log.d("TimerViewModel", "Categories refreshed: $updatedCategories")
            categories.postValue(updatedCategories)
            categoryList.postValue(updatedCategories) // Notify observers
        }
    }

    init {
        refreshCategories()
        // Load initial data from the database
        viewModelScope.launch {
            loadInitialData()
            refreshCategoryList()
        }
    }

    val activeTimers = MutableLiveData<MutableMap<Int, Boolean>>().apply {
        value = mutableMapOf()
    }
    val categoryTimes = MutableLiveData<MutableMap<Int, Long>>().apply {
        value = mutableMapOf()
    }

    private suspend fun refreshCategoryList() {
        val categories = database.categoryDao().getAllCategories()
        _categoryList.postValue(categories)
    }

    private suspend fun loadInitialData() {
        val categories = database.categoryDao().getAllCategories()
        val timesMap = categories.associate { it.id to it.totalTime }.toMutableMap()
        categoryTimes.postValue(timesMap)
    }

    fun reEmitCategoryTimes() {
        categoryTimes.value = categoryTimes.value
    }

//    fun startTimer(categoryId: Int) {
//        if (activeTimers.value?.get(categoryId) == true) return
//
//        activeTimers.value = activeTimers.value?.apply { this[categoryId] = true }
//        timerEvent.value = TimerEvent(categoryId, categoryTimes.value?.get(categoryId) ?: 0L, true)
//
//        viewModelScope.launch {
//            while (activeTimers.value?.get(categoryId) == true) {
//                delay(1000)
//                categoryTimes.value = categoryTimes.value?.apply {
//                    this[categoryId] = (this[categoryId] ?: 0) + 1
//                }
//            }
//        }
//    }

    fun startTimer(categoryId: Int) {
        if (activeTimers.value?.get(categoryId) == true) return

        activeTimers.value = activeTimers.value?.apply { this[categoryId] = true }
        timerEvent.value = TimerEvent(categoryId, categoryTimes.value?.get(categoryId) ?: 0L, true)

        viewModelScope.launch {
            while (activeTimers.value?.get(categoryId) == true) {
                delay(1000)
                categoryTimes.value = categoryTimes.value?.apply {
                    this[categoryId] = (this[categoryId] ?: 0) + 1
                }
            }
            refreshCategories() // Emit updated category data
        }
    }

//    fun stopTimer(categoryId: Int) {
//        if (activeTimers.value?.get(categoryId) == false) return
//
//        // Update in-memory state
//        activeTimers.value = activeTimers.value?.apply { this[categoryId] = false }
//        val elapsedTime = categoryTimes.value?.get(categoryId) ?: 0L
//        timerEvent.value = TimerEvent(categoryId, elapsedTime, false)
//
//        // Save the elapsed time to the database
//        viewModelScope.launch {
//            val category = database.categoryDao().getCategoryById(categoryId)
//            if (category != null) {
//                category.totalTime = elapsedTime
//                database.categoryDao().updateCategory(category)
//                // Log success for debugging
//                Log.d("TimerViewModel", "Elapsed time saved to database for categoryId: $categoryId, elapsedTime: $elapsedTime")
//            } else {
//                // Log error for debugging
//                Log.e("TimerViewModel", "No category found for categoryId: $categoryId")
//            }
//        }
//    }

    fun stopTimer(categoryId: Int) {
        if (activeTimers.value?.get(categoryId) == false) return

        activeTimers.value = activeTimers.value?.apply { this[categoryId] = false }
        timerEvent.value = TimerEvent(categoryId, categoryTimes.value?.get(categoryId) ?: 0L, false)

        // Persist timer state to the database
        viewModelScope.launch {
            val category = database.categoryDao().getCategoryById(categoryId)
            if (category != null) {
                category.totalTime = categoryTimes.value?.get(categoryId) ?: 0L
                database.categoryDao().updateCategory(category)
                refreshCategories() // Emit updated category data
            }
        }
    }

    fun loadTimerData() {
        viewModelScope.launch {
            val categories = database.categoryDao().getAllCategories()
            val timesMap = categories.associate { it.id to it.totalTime }
            categoryTimes.postValue(timesMap.toMutableMap())
        }
    }



    fun updateTimerState(categoryId: Int, elapsedTime: Long) {
        categoryTimes.value = categoryTimes.value?.apply {
            this[categoryId] = elapsedTime
        }
    }


}