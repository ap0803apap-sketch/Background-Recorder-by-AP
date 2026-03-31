package com.ap.background.recorder.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class Recording(
    val id: String,
    val name: String,
    val type: RecordingType,
    val duration: Long = 0L, // in milliseconds
    val size: Long = 0L, // in bytes
    val createdAt: Long = System.currentTimeMillis(),
    val filePath: String = ""
) : Parcelable

enum class RecordingType {
    VIDEO, AUDIO, PHOTO
}

data class VideoPreset(
    val resolution: String = "1080p",
    val fps: Int = 30,
    val camera: CameraType = CameraType.PRIMARY,
    val focus: FocusMode = FocusMode.AUTO,
    val zoom: Float = 1f
)

data class AudioPreset(
    val bitrate: Int = 128,
    val sampleRate: Int = 48000,
    val channels: Int = 2
)

data class PhotoPreset(
    val megapixel: Int = 48,
    val interval: Int = 5,
    val camera: CameraType = CameraType.PRIMARY,
    val focus: FocusMode = FocusMode.AUTO,
    val zoom: Float = 1f
)

enum class CameraType {
    PRIMARY, ULTRAWIDE, FRONT
}

enum class FocusMode {
    AUTO, MANUAL
}

data class AppSettings(
    val isDarkMode: Boolean = false,
    val isDynamicColor: Boolean = true,
    val isAmoledMode: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val recordingMode: String = "VIDEO"
)