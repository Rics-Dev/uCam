package com.ricsdev.uconnect.util

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit

private val logger = AppLogger

class VirtualCamera {

    private var ffmpegProcess: Process? = null
    private val isRunning = AtomicBoolean(false)
    private var retryCount = 0
    private val maxRetries = 3
    private val retryDelayMs = 500L // Reduced delay
    private val frameBuffer = LinkedBlockingQueue<ByteArray>() // Dynamic buffer
    private var maxBufferSize = 30 // Initial buffer size
    private var frameSkippingThreshold = 20 // Initial skipping threshold
    private var frameProcessingRate = 0L // Frames processed per second
    private var lastFrameProcessedTime = System.currentTimeMillis()

    suspend fun start(
        devicePath: String = "/dev/video5",
        cameraMode: String = "Back",
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        rotation: Float = 0f,
        maxWidth: Int = 1920,
        maxHeight: Int = 1080
    ): Unit = withContext(Dispatchers.IO) {

        if (!File(devicePath).exists()) {
            logger.i("VirtualCamera", "Loading v4l2loopback module...")
            Runtime.getRuntime().exec(arrayOf(
                "pkexec",
                "modprobe",
                "v4l2loopback",
                "devices=1",
                "video_nr=5",
                "card_label=uConnect Virtual Camera",
                "exclusive_caps=1"
            ))
            Thread.sleep(1000)
        }

        val transposeFilter = if (cameraMode == "Back") "transpose=0" else "transpose=3"
        val hflipFilter = if (!flipHorizontal) ",hflip" else ""
        val vflipFilter = if (!flipVertical) ",vflip" else "" // Inverted logic
        val rotateFilter = if (rotation != 0f) ",rotate=${rotation}*PI/180" else ""

        val scaleFilter = "scale=w='if(gt(iw/ih,${maxWidth}/${maxHeight}),${maxWidth},trunc(oh*a))':h='if(gt(iw/ih,${maxWidth}/${maxHeight}),trunc(ow/a),${maxHeight})'"

        val commandList = mutableListOf(
            "ffmpeg",
            "-f", "image2pipe",
            "-framerate", "30",
            "-i", "pipe:0",
            "-vf", "$transposeFilter$hflipFilter$vflipFilter$rotateFilter,$scaleFilter,format=yuv420p",
            "-preset", "ultrafast",
            "-tune", "zerolatency",
            "-bufsize", "512k",
            "-maxrate", "2M",
            "-g", "1",
            "-crf", "23",
            "-vsync", "0",
            "-f", "v4l2",
            devicePath
        )

        ffmpegProcess = ProcessBuilder(commandList)
            .redirectErrorStream(true)
            .start()

        isRunning.set(true)
        logger.i("VirtualCamera", "FFmpeg process started successfully.")

        // Start a coroutine to consume frames from the buffer
        withContext(Dispatchers.IO) {
            while (isRunning.get()) {
                val frame = frameBuffer.poll(100, TimeUnit.MILLISECONDS)
                if (frame != null) {
                    try {
                        ffmpegProcess?.outputStream?.write(frame)
                        ffmpegProcess?.outputStream?.flush()
                        updateFrameProcessingRate() // Update frame processing rate
                        adjustBufferSize() // Dynamically adjust buffer size
                    } catch (e: Exception) {
                        logger.e("VirtualCamera", "Error writing frame to virtual camera: ${e.message}")
                        restartProcess()
                    }
                } else if (frameBuffer.size > frameSkippingThreshold) {
                    // Dynamic frame skipping: drop frames if buffer is too full
                    logger.e("VirtualCamera", "Buffer full, dropping frames to catch up.")
                    frameBuffer.clear() // Clear buffer to reduce latency
                }
            }
        }
    }

    private fun updateFrameProcessingRate() {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastFrameProcessedTime
        if (timeDiff > 0) {
            frameProcessingRate = 1000 / timeDiff // Frames per second
        }
        lastFrameProcessedTime = currentTime
    }

    private fun adjustBufferSize() {
        // Adjust buffer size based on frame processing rate
        if (frameProcessingRate < 20) {
            // If processing rate is low, reduce buffer size to minimize latency
            maxBufferSize = 20
            frameSkippingThreshold = 15
        } else if (frameProcessingRate > 30) {
            // If processing rate is high, increase buffer size to smooth playback
            maxBufferSize = 50
            frameSkippingThreshold = 40
        } else {
            // Default values
            maxBufferSize = 30
            frameSkippingThreshold = 20
        }
    }

    suspend fun changeOrientation(
        cameraOrientation: String,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        rotation: Float = 0f
    ) = withContext(Dispatchers.IO) {
        if (!isRunning.get() || ffmpegProcess == null) {
            logger.e("VirtualCamera", "Virtual camera is not running or FFmpeg process is null.")
            return@withContext
        }
        stop() // Stop the current FFmpeg process

        // Restart the FFmpeg process with the new parameters
        start(
            cameraMode = cameraOrientation,
            flipHorizontal = flipHorizontal,
            flipVertical = flipVertical,
            rotation = rotation
        )
    }

    private suspend fun restartProcess() = withContext(Dispatchers.IO) {
        if (retryCount < maxRetries) {
            retryCount++
            logger.i("VirtualCamera", "Attempting to restart FFmpeg process (Retry $retryCount of $maxRetries)...")
            stop()
            Thread.sleep(retryDelayMs)
            start()
        } else {
            logger.e("VirtualCamera", "Max retries ($maxRetries) reached. Stopping virtual camera.")
            stop()
        }
    }

    suspend fun writeFrame(frameBytes: ByteArray) = withContext(Dispatchers.IO) {
        if (!isRunning.get() || ffmpegProcess == null) {
            logger.e("VirtualCamera", "Virtual camera is not running or FFmpeg process is null.")
            return@withContext
        }

        if (frameBytes.isEmpty()) {
            logger.e("VirtualCamera", "Empty frame bytes provided. Skipping write operation.")
            return@withContext
        }

        // Skip frames if the buffer is too full
        if (frameBuffer.size >= frameSkippingThreshold) {
            logger.e("VirtualCamera", "Frame buffer full. Skipping frame.")
            return@withContext
        }

        // Add the frame to the buffer
        frameBuffer.offer(frameBytes)
    }

    fun stop() {
        isRunning.set(false)
        ffmpegProcess?.apply {
            try {
                outputStream.close()
                destroy()
                logger.i("VirtualCamera", "FFmpeg process stopped successfully.")
            } catch (e: Exception) {
                logger.e("VirtualCamera", "Error stopping FFmpeg process: ${e.message}")
            }
        }
        ffmpegProcess = null
        retryCount = 0
        frameBuffer.clear()
    }

    fun isActive(): Boolean = isRunning.get() && ffmpegProcess != null

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.i("VirtualCamera", "Shutdown hook triggered. Stopping virtual camera...")
            stop()
        })
    }
}