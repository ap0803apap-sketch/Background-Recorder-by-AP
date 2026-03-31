package com.ap.background.recorder.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata

class CameraManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun getAvailableCameras(): List<CameraInfo> {
        val cameras = mutableListOf<CameraInfo>()

        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f

            val cameraName = when (facing) {
                CameraMetadata.LENS_FACING_FRONT -> "Front Camera"
                CameraMetadata.LENS_FACING_BACK -> "Primary Camera"
                else -> "Camera $cameraId"
            }

            cameras.add(
                CameraInfo(
                    id = cameraId,
                    name = cameraName,
                    facing = facing ?: CameraMetadata.LENS_FACING_BACK,
                    maxZoom = maxZoom
                )
            )
        }

        return cameras
    }

    fun getMaxZoom(cameraId: String): Float {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
        } catch (e: Exception) {
            1f
        }
    }

    fun getSupportedVideoResolutions(cameraId: String): List<String> {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            streamConfigMap?.getOutputSizes(android.media.MediaRecorder::class.java)?.map { size ->
                "${size.width}x${size.height}"
            }?.distinct() ?: listOf("1920x1080", "2560x1440", "3840x2160")
        } catch (e: Exception) {
            listOf("1920x1080", "2560x1440", "3840x2160")
        }
    }

    data class CameraInfo(
        val id: String,
        val name: String,
        val facing: Int,
        val maxZoom: Float
    )
}