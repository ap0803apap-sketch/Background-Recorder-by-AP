package com.ap.background.recorder.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileManager(private val context: Context) {

    private fun getBaseDir(): File {
        return context.getExternalFilesDir(null) ?: context.filesDir
    }

    fun getVideoDirectory(): File {
        return File(getBaseDir(), "Videos").apply { mkdirs() }
    }

    fun getAudioDirectory(): File {
        return File(getBaseDir(), "Audio").apply { mkdirs() }
    }

    fun getPhotoDirectory(): File {
        return File(getBaseDir(), "Pictures").apply { mkdirs() }
    }

    fun createVideoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(getVideoDirectory(), "VID_$timestamp.mp4")
    }

    fun createAudioFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(getAudioDirectory(), "AUD_$timestamp.m4a")
    }

    fun createPhotoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(getPhotoDirectory(), "IMG_$timestamp.jpg")
    }

    fun getRecordingSize(file: File): String {
        val bytes = file.length()
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    fun getAllRecordings(): List<File> {
        val allFiles = mutableListOf<File>()
        allFiles.addAll(getVideoDirectory().listFiles()?.toList() ?: emptyList())
        allFiles.addAll(getAudioDirectory().listFiles()?.toList() ?: emptyList())
        allFiles.addAll(getPhotoDirectory().listFiles()?.toList() ?: emptyList())
        return allFiles.sortedByDescending { it.lastModified() }
    }

    fun saveToDownloads(file: File, feature: String): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(file.lastModified()))
        val appName = "Background Recorder"
        val fileName = "${timestamp}_${feature}_$appName.${file.extension}"
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$appName/$feature"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(file))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
        }

        val contentResolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            // Legacy support would involve manual file copying to Downloads, 
            // but for simplicity on modern SDKs:
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI // Fallback
        }

        val uri = contentResolver.insert(collection, contentValues)
        
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    FileInputStream(file).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                return it
            } catch (e: Exception) {
                Log.e("FileManager", "Error saving to downloads", e)
            }
        }
        return null
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "mp4" -> "video/mp4"
            "m4a" -> "audio/mp4"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "*/*"
        }
    }
}