package com.ricsdev.ucam.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class VirtualCamera {

    // Logger for better log management
    private val logger = LoggerFactory.getLogger(VirtualCamera::class.java)

    private var ffmpegProcess: Process? = null
    private val isRunning = AtomicBoolean(false)

    // Retry configuration for process restart
    private var retryCount = 0
    private val maxRetries = 3
    private val retryDelayMs = 2000L // 2 seconds delay between retries

    suspend fun start(devicePath: String = "/dev/video5") = withContext(Dispatchers.IO) {
//        if (!Platform.isLinux()) {
//            throw UnsupportedOperationException("Virtual camera currently only supports Linux")
//        }

        // Validate devicePath
        if (devicePath.isBlank() || !File(devicePath).exists()) {
            logger.error("Invalid device path: $devicePath")
            throw IllegalArgumentException("Invalid device path: $devicePath")
        }

        // Load v4l2loopback if device doesn't exist
        if (!File(devicePath).exists()) {
            logger.info("Loading v4l2loopback module...")
            Runtime.getRuntime().exec(arrayOf("sudo", "modprobe", "v4l2loopback"))
            Thread.sleep(1000) // Wait for device creation
        }

        // Start FFmpeg process with settings for receiving piped input
        val commandList = listOf(
            "ffmpeg",
            "-f", "image2pipe",
            "-framerate", "30",
            "-i", "pipe:0",
            "-vf", "transpose=2,vflip,format=yuv420p",
            "-preset", "ultrafast",
            "-f", "v4l2",
            devicePath
        )

        ffmpegProcess = ProcessBuilder(commandList)
            .redirectErrorStream(true)
            .start()

        isRunning.set(true)
        logger.info("FFmpeg process started successfully.")
    }

    private fun isProcessAlive(): Boolean {
        return try {
            ffmpegProcess?.exitValue()
            false
        } catch (e: IllegalThreadStateException) {
            true
        }
    }

    suspend fun writeFrame(frameBytes: ByteArray) = withContext(Dispatchers.IO) {
        if (!isRunning.get() || ffmpegProcess == null) {
            logger.warn("Virtual camera is not running or FFmpeg process is null.")
            return@withContext
        }

        // Validate frameBytes
        if (frameBytes.isEmpty()) {
            logger.warn("Empty frame bytes provided. Skipping write operation.")
            return@withContext
        }

        try {
            ffmpegProcess?.outputStream?.write(frameBytes)
            ffmpegProcess?.outputStream?.flush()
            logger.debug("Frame written to virtual camera successfully.")
        } catch (e: Exception) {
            logger.error("Error writing frame to virtual camera: ${e.message}", e)

            // Only attempt restart if process is actually dead
            if (!isProcessAlive()) {
                if (retryCount < maxRetries) {
                    retryCount++
                    logger.info("Attempting to restart FFmpeg process (Retry $retryCount of $maxRetries)...")
                    stop()
                    Thread.sleep(retryDelayMs) // Add delay before restarting
                    start()
                } else {
                    logger.error("Max retries ($maxRetries) reached. Stopping virtual camera.")
                    stop()
                }
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        ffmpegProcess?.apply {
            try {
                outputStream.close()
                destroy()
                logger.info("FFmpeg process stopped successfully.")
            } catch (e: Exception) {
                logger.error("Error stopping FFmpeg process: ${e.message}", e)
            }
        }
        ffmpegProcess = null
        retryCount = 0 // Reset retry count on stop
    }

    fun isActive(): Boolean = isRunning.get() && ffmpegProcess != null

    // Graceful shutdown hook
    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutdown hook triggered. Stopping virtual camera...")
            stop()
        })
    }
}