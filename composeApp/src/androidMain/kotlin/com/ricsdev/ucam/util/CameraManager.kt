package com.ricsdev.ucam.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.YuvImage
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

    suspend fun startCamera(
        previewView: PreviewView,
        onFrameCapture: (ByteArray) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProvider = withContext(Dispatchers.IO) {
            cameraProviderFuture.get()
        }

        val preview = Preview.Builder().build()
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(mainExecutor) { imageProxy ->
                    try {
                        if (imageProxy.format == ImageFormat.YUV_420_888) {
                            // Convert YUV_420_888 to RGB Bitmap
                            val bitmap = imageProxy.toBitmap() // This handles the conversion
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
//        val imageAnalyzer = ImageAnalysis.Builder()
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//            .apply {
//                setAnalyzer(mainExecutor) { imageProxy ->
//                    try {
//                        // Check the image format using ImageProxy.getFormat()
//                        if (imageProxy.format == ImageFormat.YUV_420_888) {
//                            val planes = imageProxy.planes
//                            val yBuffer = planes[0].buffer
//                            val uBuffer = planes[1].buffer
//                            val vBuffer = planes[2].buffer
//
//                            val ySize = yBuffer.remaining()
//                            val uSize = uBuffer.remaining()
//                            val vSize = vBuffer.remaining()
//
//                            val nv21 = ByteArray(ySize + uSize + vSize)
//
//                            // Copy Y plane
//                            yBuffer.get(nv21, 0, ySize)
//                            // Interleave U and V planes
//                            var pos = ySize
//                            // Copy U and V planes
//                            uBuffer.get(nv21, ySize, uSize)
//                            vBuffer.get(nv21, ySize + uSize, vSize)
//
//                            // Convert to JPEG
//                            val yuvImage = YuvImage(
//                                nv21,
//                                ImageFormat.NV21,
//                                imageProxy.width,
//                                imageProxy.height,
//                                null
//                            )
//
//                            val out = ByteArrayOutputStream()
//                            yuvImage.compressToJpeg(
//                                android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
//                                80,
//                                out
//                            )
//
//                            val jpegBytes = out.toByteArray()
//                            onFrameCapture(jpegBytes)
//                        } else {
//                            Log.w("CameraManager", "Unsupported image format: ${imageProxy.format}")
//                        }
//                    } catch (e: Exception) {
//                        Log.e("CameraManager", "Error processing image", e)
//                    } finally {
//                        imageProxy.close()
//                    }
//                }
//            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                context as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            preview.surfaceProvider = previewView.surfaceProvider
        } catch (e: Exception) {
            Log.e("CameraManager", "Use case binding failed", e)
        }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
    }
}

