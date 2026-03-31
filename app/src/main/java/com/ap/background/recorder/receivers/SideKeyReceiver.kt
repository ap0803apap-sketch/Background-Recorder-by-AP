package com.ap.background.recorder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ap.background.recorder.MainActivity
import com.ap.background.recorder.services.RecordingService

class SideKeyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            // Samsung Side Key Event
            "com.samsung.accessory.action.SIDE_KEY_EVENT" -> {
                handleSideKeyEvent(context, intent)
            }
            // Device Admin Action
            "android.intent.action.DEVICE_ADMIN_ENABLED" -> {
                handleDeviceAdminEnabled(context)
            }
        }
    }

    private fun handleSideKeyEvent(context: Context, intent: Intent) {
        // Get key code (usually side key is KeyEvent.KEYCODE_SIDE_KEY on Samsung devices)
        val keyCode = intent.getIntExtra("keyCode", -1)
        val keyEvent = intent.getIntExtra("keyEvent", -1)

        // Check for press (keyEvent = 0) or release (keyEvent = 1)
        if (keyCode == SIDE_KEY_CODE && keyEvent == KEY_PRESS) {
            // Launch the app or start recording
            startRecorderApp(context)
        }
    }

    private fun handleDeviceAdminEnabled(context: Context) {
        // Device admin was enabled
        // You can initialize any device admin features here
    }

    private fun startRecorderApp(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        context.startActivity(intent)
    }

    companion object {
        // Samsung Side Key Code (can vary by device)
        private const val SIDE_KEY_CODE = 254
        private const val KEY_PRESS = 0
    }
}