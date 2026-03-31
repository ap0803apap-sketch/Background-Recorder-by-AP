package com.ap.background.recorder.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.ap.background.recorder.MainActivity
import com.ap.background.recorder.R
import com.ap.background.recorder.data.RecorderPreferences
import com.ap.background.recorder.utils.FileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecordingService : LifecycleService() {

    private val NOTIFICATION_ID = 1001
    private val NOTIFICATION_CHANNEL_ID = "recording_channel_v3"
    private val TAG = "RecordingService"

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fileManager: FileManager
    private lateinit var prefs: RecorderPreferences
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    private var currentAction: String? = null
    private var photoHandler: Handler? = null
    private var photoRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()
        fileManager = FileManager(this)
        prefs = RecorderPreferences(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.action ?: return START_NOT_STICKY

        if (action == ACTION_STOP) {
            stopRecording()
            return START_NOT_STICKY
        }

        if (currentAction == action) {
            stopRecording()
            return START_NOT_STICKY
        }

        if (currentAction != null) {
            stopRecording()
        }

        currentAction = action

        val requiredType = when (action) {
            ACTION_START_VIDEO -> ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            ACTION_START_AUDIO -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            ACTION_START_PHOTO -> ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            else -> 0
        }

        try {
            val notification = createNotification("Initializing...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && requiredType != 0) {
                startForeground(NOTIFICATION_ID, notification, requiredType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            when (action) {
                ACTION_START_VIDEO -> startVideoRecording()
                ACTION_START_PHOTO -> startPhotoCaptureInterval()
                ACTION_START_AUDIO -> startVideoRecording() 
            }
        }

        return START_STICKY
    }

    private suspend fun startVideoRecording() {
        updateNotification("Video recording in progress...")
        val cameraSelection = prefs.getCameraSelection()
        val zoomLevel = prefs.getZoomLevel()
        
        val resolution = if (cameraSelection == "FRONT") prefs.getFrontVideoResolution() else prefs.getVideoResolution()
        val quality = when (resolution) {
            "4K" -> Quality.UHD
            "1080p" -> Quality.FHD
            "720p" -> Quality.HD
            else -> Quality.HIGHEST
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(quality))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            
            val cameraSelector = getCameraSelector(cameraProvider, cameraSelection)

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture)
                camera?.cameraControl?.setZoomRatio(zoomLevel)

                val outputOptions = FileOutputOptions.Builder(fileManager.createVideoFile()).build()
                recording = videoCapture?.output
                    ?.prepareRecording(this, outputOptions)
                    ?.apply { 
                        if (ContextCompat.checkSelfPermission(this@RecordingService, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            withAudioEnabled()
                        }
                    }
                    ?.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                        if (recordEvent is VideoRecordEvent.Finalize && recordEvent.hasError()) {
                            stopRecording()
                        }
                    }
            } catch (e: Exception) {
                stopRecording()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private suspend fun startPhotoCaptureInterval() {
        val intervalSeconds = prefs.getPhotoInterval()
        val cameraSelection = prefs.getCameraSelection()
        val zoomLevel = prefs.getZoomLevel()
        
        updateNotification("Taking photos every $intervalSeconds seconds")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = getCameraSelector(cameraProvider, cameraSelection)

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
                camera?.cameraControl?.setZoomRatio(zoomLevel)

                photoHandler = Handler(Looper.getMainLooper())
                photoRunnable = object : Runnable {
                    override fun run() {
                        takeSinglePhoto()
                        photoHandler?.postDelayed(this, intervalSeconds * 1000L)
                    }
                }
                photoHandler?.post(photoRunnable!!)
                
            } catch (e: Exception) {
                stopRecording()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takeSinglePhoto() {
        val outputOptions = ImageCapture.OutputFileOptions.Builder(fileManager.createPhotoFile()).build()
        imageCapture?.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Photo saved: ${output.savedUri}")
                }
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo failed: ${exc.message}")
                }
            }
        )
    }

    private fun getCameraSelector(cameraProvider: ProcessCameraProvider, selection: String): CameraSelector {
        val backCameras = cameraProvider.availableCameraInfos.filter { it.lensFacing == CameraSelector.LENS_FACING_BACK }
        val frontCameras = cameraProvider.availableCameraInfos.filter { it.lensFacing == CameraSelector.LENS_FACING_FRONT }

        return when (selection) {
            "PRIMARY" -> {
                if (backCameras.isNotEmpty()) {
                    CameraSelector.Builder().addCameraFilter { it.filter { info -> info == backCameras[0] } }.build()
                } else CameraSelector.DEFAULT_BACK_CAMERA
            }
            "SECONDARY" -> {
                if (backCameras.size > 1) {
                    CameraSelector.Builder().addCameraFilter { it.filter { info -> info == backCameras[1] } }.build()
                } else {
                    if (backCameras.isNotEmpty()) {
                        CameraSelector.Builder().addCameraFilter { it.filter { info -> info == backCameras[0] } }.build()
                    } else CameraSelector.DEFAULT_BACK_CAMERA
                }
            }
            "FRONT" -> {
                if (frontCameras.isNotEmpty()) {
                    CameraSelector.Builder().addCameraFilter { it.filter { info -> info == frontCameras[0] } }.build()
                } else CameraSelector.DEFAULT_FRONT_CAMERA
            }
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    private fun stopRecording() {
        photoHandler?.removeCallbacks(photoRunnable ?: Runnable {})
        recording?.stop()
        recording = null
        imageCapture = null
        currentAction = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Recorder", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Background Recorder")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_recorder)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(content))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    companion object {
        const val ACTION_START_VIDEO = "com.ap.background.recorder.START_VIDEO"
        const val ACTION_START_AUDIO = "com.ap.background.recorder.START_AUDIO"
        const val ACTION_START_PHOTO = "com.ap.background.recorder.START_PHOTO"
        const val ACTION_STOP = "com.ap.background.recorder.STOP"
    }
}