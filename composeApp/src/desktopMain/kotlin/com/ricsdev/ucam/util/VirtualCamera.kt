package com.ricsdev.ucam.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class VirtualCamera {

    private var ffmpegProcess: Process? = null
    private val isRunning = AtomicBoolean(false)
    private var retryCount = 0
    private val maxRetries = 3
    private val retryDelayMs = 500L // Reduced delay

    suspend fun start(
        devicePath: String = "/dev/video5",
        cameraMode: String = "Back",
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        rotation: Float = 0f
    ) = withContext(Dispatchers.IO) {

        if (!File(devicePath).exists()) {
            println("Loading v4l2loopback module...")
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
        val hflipFilter = if (flipHorizontal) ",hflip" else ""
        val vflipFilter = if (!flipVertical) ",vflip" else "" // Inverted logic
        val rotateFilter = if (rotation != 0f) ",rotate=${rotation}*PI/180" else ""

        val commandList = listOf(
            "ffmpeg",
            "-f", "image2pipe",
            "-framerate", "30",
            "-i", "pipe:0",
            "-vf", "$transposeFilter$hflipFilter$vflipFilter$rotateFilter,format=yuv420p",
            "-preset", "ultrafast",
            "-f", "v4l2",
            devicePath
        )

        ffmpegProcess = ProcessBuilder(commandList)
            .redirectErrorStream(true)
            .start()

        isRunning.set(true)
        println("FFmpeg process started successfully.")
    }

    suspend fun changeOrientation(
        cameraOrientation: String,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        rotation: Float = 0f
    ) = withContext(Dispatchers.IO) {
        if (!isRunning.get() || ffmpegProcess == null) {
            println("Virtual camera is not running or FFmpeg process is null.")
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
            println("Attempting to restart FFmpeg process (Retry $retryCount of $maxRetries)...")
            stop()
            Thread.sleep(retryDelayMs)
            start()
        } else {
            println("Max retries ($maxRetries) reached. Stopping virtual camera.")
            stop()
        }
    }

    suspend fun writeFrame(frameBytes: ByteArray) = withContext(Dispatchers.IO) {
        if (!isRunning.get() || ffmpegProcess == null) {
            println("Virtual camera is not running or FFmpeg process is null.")
            return@withContext
        }

        if (frameBytes.isEmpty()) {
            println("Empty frame bytes provided. Skipping write operation.")
            return@withContext
        }

        try {
            ffmpegProcess?.outputStream?.write(frameBytes)
            ffmpegProcess?.outputStream?.flush()
            println("Frame written to virtual camera successfully.")
        } catch (e: Exception) {
            println("Error writing frame to virtual camera: ${e.message}")
            restartProcess()
        }
    }

    fun stop() {
        isRunning.set(false)
        ffmpegProcess?.apply {
            try {
                outputStream.close()
                destroy()
                println("FFmpeg process stopped successfully.")
            } catch (e: Exception) {
                println("Error stopping FFmpeg process: ${e.message}")
            }
        }
        ffmpegProcess = null
        retryCount = 0
    }

    fun isActive(): Boolean = isRunning.get() && ffmpegProcess != null

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            println("Shutdown hook triggered. Stopping virtual camera...")
            stop()
        })
    }
}