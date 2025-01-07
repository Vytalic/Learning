package com.vytalitech.android.timekeeper

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    var totalTime: Long = 0,
    val date: String
)
