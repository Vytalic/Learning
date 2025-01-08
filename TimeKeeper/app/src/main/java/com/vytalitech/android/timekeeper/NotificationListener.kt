package com.vytalitech.android.timekeeper

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener : NotificationListenerService() {
    companion object {
        var isNotificationPanelVisible = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // User opened the notification
        isNotificationPanelVisible = true
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // User closed the notification
        isNotificationPanelVisible = false
    }
}
