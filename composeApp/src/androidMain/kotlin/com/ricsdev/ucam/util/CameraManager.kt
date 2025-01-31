package com.ricsdev.ucam.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class CameraManager(private val context: Context) {
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private var currentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var currentRotation = 0f

    suspend fun startCamera(
        previewView: PreviewView,
        useFrontCamera: Boolean = false,
        onFrameCapture: (ByteArray) -> Unit
    ) {
        currentCameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProvider = withContext(Dispatchers.IO) {
            cameraProviderFuture.get()
        }

        val preview = Preview.Builder()
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(mainExecutor) { imageProxy ->
                    try {
                        if (imageProxy.format == ImageFormat.YUV_420_888) {
                            val bitmap = imageProxy.toBitmap()
                            val out = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                            val jpegBytes = out.toByteArray()
                            onFrameCapture(jpegBytes)
                        } else {
                            Log.w("CameraManager", "Unsupported image format: ${imageProxy.format}")
                        }
                    } catch (e: Exception) {
                        Log.e("CameraManager", "Error processing image", e)
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                context as LifecycleOwner,
                currentCameraSelector,
                preview,
                imageAnalyzer
            )
            preview.surfaceProvider = previewView.surfaceProvider
        } catch (e: Exception) {
            Log.e("CameraManager", "Use case binding failed", e)
        }
    }

    fun switchCamera(useFrontCamera: Boolean) {
        camera?.cameraInfo?.let {
            val newCameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            if (currentCameraSelector != newCameraSelector) {
                currentCameraSelector = newCameraSelector
                cameraProvider?.unbindAll()
                // Camera will be restarted with new selector in the next startCamera call
            }
        }
    }

    fun rotateCamera(previewView: PreviewView, rotation: Float) {
        currentRotation = (currentRotation + rotation) % 360
        previewView.rotation = currentRotation
    }

    fun flipHorizontal(previewView: PreviewView) {
        previewView.scaleX = if (previewView.scaleX == 1f) -1f else 1f
    }

    fun flipVertical(previewView: PreviewView) {
        previewView.scaleY = if (previewView.scaleY == 1f) -1f else 1f
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
    }
}