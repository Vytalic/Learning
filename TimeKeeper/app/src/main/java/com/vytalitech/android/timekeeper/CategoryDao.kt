package com.vytalitech.android.timekeeper

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY `order` ASC")
    fun getAllCategories(): LiveData<List<Category>> // Fetch categories sorted by their order

    @Update
    suspend fun updateCategory(category: Category)

    @Update
    suspend fun updateCategories(categories: List<Category>)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: Int): Category?

    @Query("SELECT * FROM categories WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getCategoriesByDateRange(startDate: String, endDate: String): List<Category>

    @Query("SELECT MAX(`order`) FROM categories")
    suspend fun getMaxOrder(): Int?

    @Query("SELECT * FROM categories ORDER BY `order` ASC")
    suspend fun getAllCategoriesDirect(): List<Category>


}