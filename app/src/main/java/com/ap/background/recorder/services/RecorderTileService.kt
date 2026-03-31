package com.ap.background.recorder.services

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class RecorderTileService : TileService() {

    override fun onClick() {
        super.onClick()

        val serviceIntent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START_VIDEO // Or get preferred mode from DataStore if needed
        }

        // Check if service is already running by checking tile state (simplified)
        if (qsTile.state == Tile.STATE_ACTIVE) {
            serviceIntent.action = RecordingService.ACTION_STOP
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.label = "Start Recording"
        } else {
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.label = "Stop Recording"
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        // Here you could check actual service state and sync tile
    }
}