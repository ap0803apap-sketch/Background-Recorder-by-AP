package com.ap.background.recorder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ap.background.recorder.data.RecorderPreferences
import com.ap.background.recorder.services.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if ((context == null) || (intent == null)) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        val prefs = RecorderPreferences(context)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                if (!prefs.smsTriggerEnabledFlow.first()) return@launch

                val triggerText = prefs.smsTriggerTextFlow.first().trim()
                if (triggerText.isEmpty()) return@launch

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val fullMessage = messages.joinToString("") { it.displayMessageBody ?: "" }
                
                if (fullMessage.contains(triggerText, ignoreCase = true)) {
                    startRecording(context, prefs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun startRecording(context: Context, prefs: RecorderPreferences) {
        val action = when (prefs.getRecordingMode()) {
            "VIDEO" -> RecordingService.ACTION_START_VIDEO
            "PHOTO" -> RecordingService.ACTION_START_PHOTO
            "AUDIO" -> RecordingService.ACTION_START_AUDIO
            else -> RecordingService.ACTION_START_VIDEO
        }

        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            this.action = action
            this.putExtra("allow_toggle", prefs.isSmsToggleOffEnabled())
        }
        
        try {
            context.startForegroundService(serviceIntent)
        } catch (_: Exception) {
            // Fallback for some devices or background restrictions
            context.startService(serviceIntent)
        }
    }
}
