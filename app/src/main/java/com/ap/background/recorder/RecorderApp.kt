package com.ap.background.recorder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class RecorderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Recording Service Channel
            val recordingChannel = NotificationChannel(
                "recording_channel",
                "Recording Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for background recording"
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(recordingChannel)

            // App Updates Channel
            val updatesChannel = NotificationChannel(
                "updates_channel",
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for app updates and status"
            }
            notificationManager.createNotificationChannel(updatesChannel)
        }
    }
}