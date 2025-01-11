package com.vytalitech.android.timekeeper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale


class TimerService : Service() {
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val timerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val activeTimers = mutableMapOf<Int, Long>() // Category ID -> Elapsed Time

    companion object {
        const val CHANNEL_ID = "timer_channel"

        fun startService(context: Context, categoryId: Int, categoryName: String) {
            val intent = Intent(context, TimerService::class.java).apply {
                putExtra("CATEGORY_ID", categoryId)
                putExtra("CATEGORY_NAME", categoryName)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val categoryId = intent?.getIntExtra("CATEGORY_ID", -1) ?: -1
        val categoryName = intent?.getStringExtra("CATEGORY_NAME") ?: "Unknown"

        if (categoryId != -1 && !activeTimers.containsKey(categoryId)) {
            startTimer(categoryId, categoryName)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        timerScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTimer(categoryId: Int, categoryName: String) {
        activeTimers[categoryId] = 0L

        timerScope.launch {
            while (true) {
                delay(1000)
                activeTimers[categoryId] = (activeTimers[categoryId] ?: 0L) + 1
                updateNotification(categoryId, categoryName)
            }
        }
    }

    private fun updateNotification(categoryId: Int, categoryName: String) {
        val elapsedSeconds = activeTimers[categoryId] ?: 0L
        val formattedTime = formatTime(elapsedSeconds)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer Running")
            .setContentText("$categoryName: $formattedTime")
            .setSmallIcon(R.drawable.ic_time) // Replace with your app's timer icon
            .setOngoing(true)
            .build()

        startForeground(categoryId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for active timers"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
    }
}
