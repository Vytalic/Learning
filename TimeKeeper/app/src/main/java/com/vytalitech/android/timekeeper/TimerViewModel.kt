package com.vytalitech.android.timekeeper

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerViewModel(private val database: AppDatabase) : ViewModel() {
    val activeTimers = MutableLiveData<MutableMap<Int, Boolean>>().apply {
        value = mutableMapOf()
    }
    val categoryTimes = MutableLiveData<MutableMap<Int, Long>>().apply {
        value = mutableMapOf()
    }

    init {
        // Load initial data from the database
        viewModelScope.launch {
            loadInitialData()
        }
    }

    private suspend fun loadInitialData() {
        val categories = database.categoryDao().getAllCategories()
        val timesMap = categories.associate { it.id to it.totalTime }.toMutableMap()
        categoryTimes.postValue(timesMap)
    }

    fun reEmitCategoryTimes() {
        categoryTimes.value = categoryTimes.value
    }

    fun startTimer(categoryId: Int) {
        if (activeTimers.value?.get(categoryId) == true) return

        activeTimers.value = activeTimers.value?.apply { this[categoryId] = true }
        viewModelScope.launch {
            while (activeTimers.value?.get(categoryId) == true) {
                categoryTimes.value = categoryTimes.value?.apply {
                    this[categoryId] = (this[categoryId] ?: 0) + 1
                }

                // Persist totalTime in the database
                val category = database.categoryDao().getCategoryById(categoryId)
                category?.let {
                    it.totalTime = categoryTimes.value?.get(categoryId) ?: 0
                    database.categoryDao().updateCategory(it)
                }

                delay(1000)
            }
        }
    }

    fun stopTimer(categoryId: Int) {
        activeTimers.value = activeTimers.value?.apply { this[categoryId] = false }
    }
}