package com.vytalitech.android.timekeeper

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Category::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
}

// Migration from version 1 to version 2
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // add the 'date' column to the 'categories' table
        db.execSQL("ALTER TABLE categories ADD COLUMN date TEXT DEFAULT '1970-01-01' NOT NULL")
    }
}

// New migration from version 2 to version 3 to add the 'startTime' column
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN startTime INTEGER DEFAULT NULL")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
    }
}


object DatabaseProvider {
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
            INSTANCE = instance
            instance
        }
    }
}