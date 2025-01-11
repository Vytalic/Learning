package com.vytalitech.android.timekeeper

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TimerViewModelFactory(
    private val application: Application,
    private val database: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimerViewModel(application, database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
