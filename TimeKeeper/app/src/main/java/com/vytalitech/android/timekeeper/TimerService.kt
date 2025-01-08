package com.vytalitech.android.timekeeper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong


class TimerService : Service() {

    private val channelId = "TimerServiceChannel"

    private var categoryId: Int = -1 // Current category ID being timed
    val elapsedTime = AtomicLong(0)

    private var job: Job? = null // Reference to the coroutine running the timer

    // Reference to your database
    private lateinit var database: AppDatabase

    // Binder to provide service instance to the client
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = TimerBinder()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        database = DatabaseProvider.getDatabase(applicationContext) // Initialize the database
        createNotificationChannel()
        Log.d("TimerService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        categoryId = intent?.getIntExtra("CATEGORY_ID", -1) ?: -1
        val categoryName = intent?.getStringExtra("CATEGORY_NAME") ?: "Timer"
        val initialElapsedTime = intent?.getLongExtra("ELAPSED_TIME", 0L) ?: 0L

        if (categoryId == -1) {
            Log.e("TimerService", "Invalid category ID")
            stopSelf()
            return START_NOT_STICKY
        }

        elapsedTime.set(initialElapsedTime) // Initialize elapsed time from intent

        Log.d("TimerService", "Service started for categoryId: $categoryId with elapsed time: ${elapsedTime.get()}")

        startTimer(categoryName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, buildNotification(categoryName, elapsedTime.get()), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, buildNotification(categoryName, elapsedTime.get()))
        }

        return START_STICKY
    }




    private fun startTimer(categoryName: String) {
        if (job == null) {
            job = CoroutineScope(Dispatchers.IO).launch {
                var syncCounter = 0
                while (true) {
                    delay(1000)
                    elapsedTime.incrementAndGet()

                    if (NotificationListener.isNotificationPanelVisible) {
                        updateNotification(categoryName, elapsedTime.get())
                    }

                    // Save to database every 30 seconds
                    syncCounter++
                    if (syncCounter >= 30) {
                        saveElapsedTimeToDatabase(categoryId, elapsedTime.get())
                        syncCounter = 0
                    }
                }
            }
        } else {
            Log.d("TimerService", "Timer already running for categoryId: $categoryId")
        }
    }




    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        job = null
        Log.d("TimerService", "Service destroyed")
        saveElapsedTimeToDatabase(categoryId, elapsedTime.get())
    }




    // Cleanup mechanism
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        Log.d("TimerService", "onTaskRemoved called. Saving changes and stopping timer.")

        if (categoryId != -1) {
            saveElapsedTimeToDatabase(categoryId, elapsedTime.get())
        } else {
            Log.e("TimerService", "Cannot save elapsed time. Invalid categoryId: $categoryId")
        }

        job?.cancel()
        stopForeground(true)
        stopSelf()

        Log.d("TimerService", "App closed. Timer stopped, changes saved, notification removed.")
    }





    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channelId = "TimerServiceChannel"
        val channelName = "Timer Service Channel"
        val serviceChannel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }


    private fun buildNotification(categoryName: String, elapsedTime: Long): Notification {
        return NotificationCompat.Builder(this, "TimerServiceChannel")
            .setContentTitle("Task Timer: $categoryName")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .setWhen(System.currentTimeMillis() - elapsedTime * 1000) // Automatically adjusts chronometer
            .setUsesChronometer(true) // Dynamically shows elapsed time
            .build()
    }

    private fun updateNotification(categoryName: String, elapsedTime: Long) {
        val notification = buildNotification(categoryName, elapsedTime)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun loadElapsedTimeFromDatabase(categoryId: Int): Long {
        // Simulate database access (replace this with actual DB call)
        Log.d("TimerService", "Loading elapsed time from database for categoryId: $categoryId")
        return 0L // Replace with actual database query result
    }

    fun saveElapsedTimeToDatabase(categoryId: Int, elapsedTime: Long) {
        if (categoryId == -1) {
            Log.e("TimerService", "Cannot save elapsed time. Invalid categoryId: $categoryId")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val category = database.categoryDao().getCategoryById(categoryId)
                category?.let {
                    it.totalTime = elapsedTime
                    database.categoryDao().updateCategory(it)
                    Log.d("TimerService", "Database update successful for categoryId: $categoryId")
                } ?: Log.e("TimerService", "No category found for categoryId: $categoryId")
            } catch (e: Exception) {
                Log.e("TimerService", "Error saving elapsed time to database: ${e.message}")
            }
        }

    }

}