package com.ricsdev.ucam.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.ArrayDeque

class CameraConfig(private val context: Context) {
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private var currentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var currentRotation = 0f
    private var onFrameCaptureCallback: ((ByteArray) -> Unit)? = null

    // Frame processing
    private val frameChannel = Channel<ByteArray>(Channel.BUFFERED)
    private var lastFrameTime = 0L

    // Internal managers as properties instead of separate classes
    private val framePool = object {
        private val maxPoolSize = 5
        private val pool = ArrayDeque<ByteBuffer>(maxPoolSize)
        private val maxFrameSize = 1024 * 1024 // 1MB initial size

        @Synchronized
        fun acquire(): ByteBuffer {
            return if (pool.isEmpty()) {
                ByteBuffer.allocateDirect(maxFrameSize)
            } else {
                pool.removeFirst().apply { clear() }
            }
        }

        @Synchronized
        fun release(buffer: ByteBuffer) {
            if (pool.size < maxPoolSize) {
                buffer.clear()
                pool.addLast(buffer)
            }
        }
    }

    private val frameRateManager = object {
        private var lastFrameTime = System.nanoTime()
        private val targetFrameInterval = (1000000000.0 / 30.0).toLong() // 30 FPS
        private var frameDropThreshold = 1.5
        private var consecutiveDrops = 0
        private val maxConsecutiveDrops = 3
        private var frameCounter = 0
        private val fpsAdjustInterval = 30
        private var totalLatency = 0L

        fun shouldProcessFrame(): Boolean {
            val currentTime = System.nanoTime()
            val elapsed = currentTime - lastFrameTime

            frameCounter++
            if (frameCounter >= fpsAdjustInterval) {
                val avgLatency = totalLatency / fpsAdjustInterval
                adjustThresholds(avgLatency)
                frameCounter = 0
                totalLatency = 0
            }

            if (elapsed < targetFrameInterval * frameDropThreshold) {
                consecutiveDrops = 0
                lastFrameTime = currentTime
                return true
            }

            if (consecutiveDrops >= maxConsecutiveDrops) {
                consecutiveDrops = 0
                lastFrameTime = currentTime
                return true
            }

            consecutiveDrops++
            return false
        }

        fun adjustThresholds(avgLatency: Long) {
            frameDropThreshold = when {
                avgLatency > 500 -> 2.5
                avgLatency > 250 -> 2.0
                avgLatency > 100 -> 1.75
                else -> 1.5
            }
        }
    }

    private val qualityManager = object {
        private var currentQuality = 80
        private val minQuality = 20
        private val maxQuality = 90

        fun adjustQuality(latency: Long): Int {
            currentQuality = when {
                latency > 500 -> (currentQuality - 20).coerceAtLeast(minQuality)
                latency > 300 -> (currentQuality - 10).coerceAtLeast(minQuality)
                latency < 100 -> (currentQuality + 10).coerceAtMost(maxQuality)
                else -> currentQuality
            }
            return currentQuality
        }
    }

    suspend fun startCamera(
        previewView: PreviewView,
        useFrontCamera: Boolean,
        lifecycleOwner: LifecycleOwner,
        onFrameCapture: (ByteArray) -> Unit
    ) {
        onFrameCaptureCallback = onFrameCapture

        currentCameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

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
                        if (frameRateManager.shouldProcessFrame() &&
                            imageProxy.format == ImageFormat.YUV_420_888
                        ) {
                            processFrame(imageProxy)
                        }
                    } catch (e: Exception) {
                        Log.e("CameraManager", "Error processing image", e)
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            processFrames()
        }

        try {
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                currentCameraSelector,
                preview,
                imageAnalyzer
            )
            preview.surfaceProvider = previewView.surfaceProvider
        } catch (e: Exception) {
            Log.e("CameraManager", "Use case binding failed", e)
        }
    }

    private fun processFrame(imageProxy: ImageProxy) {
        val buffer = framePool.acquire()
        try {
            val bitmap = imageProxy.toBitmap()
            buffer.clear()

            val latency = System.currentTimeMillis() - lastFrameTime
            val quality = qualityManager.adjustQuality(latency)

            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                buffer.asOutputStream()
            )

            buffer.flip()
            val frameData = ByteArray(buffer.remaining())
            buffer.get(frameData)

            frameChannel.trySend(frameData)

            lastFrameTime = System.currentTimeMillis()
        } finally {
            framePool.release(buffer)
        }
    }

    private fun ByteBuffer.asOutputStream(): OutputStream {
        return object : OutputStream() {
            override fun write(b: Int) {
                put(b.toByte())
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                put(b, off, len)
            }
        }
    }

    private suspend fun processFrames() {
        for (frame in frameChannel) {
            try {
                withTimeout(100) {
                    onFrameCaptureCallback?.invoke(frame)
                }
            } catch (e: TimeoutCancellationException) {
                Log.w("CameraManager", "Frame processing timed out, skipping frame")
            }
        }
    }

    fun updateNetworkLatency(latency: Long) {
        frameRateManager.adjustThresholds(latency)
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