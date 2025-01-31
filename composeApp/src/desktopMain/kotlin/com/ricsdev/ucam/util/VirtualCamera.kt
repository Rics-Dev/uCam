package com.ricsdev.ucam.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class VirtualCamera {

    private val logger = LoggerFactory.getLogger(VirtualCamera::class.java)
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
        if (devicePath.isBlank() || !File(devicePath).exists()) {
            logger.error("Invalid device path: $devicePath")
            throw IllegalArgumentException("Invalid device path: $devicePath")
        }

        if (!File(devicePath).exists()) {
            logger.info("Loading v4l2loopback module...")
            Runtime.getRuntime().exec(arrayOf("sudo", "modprobe", "v4l2loopback"))
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
        logger.info("FFmpeg process started successfully.")
    }

    suspend fun changeOrientation(
        cameraOrientation: String,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        rotation: Float = 0f
    ) = withContext(Dispatchers.IO) {
        if (!isRunning.get() || ffmpegProcess == null) {
            logger.warn("Virtual camera is not running or FFmpeg process is null.")
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
            logger.info("Attempting to restart FFmpeg process (Retry $retryCount of $maxRetries)...")
            stop()
            Thread.sleep(retryDelayMs)
            start()
        } else {
            logger.error("Max retries ($maxRetries) reached. Stopping virtual camera.")
            stop()
        }
    }

    suspend fun writeFrame(frameBytes: ByteArray) = withContext(Dispatchers.IO) {
        if (!isRunning.get() || ffmpegProcess == null) {
            logger.warn("Virtual camera is not running or FFmpeg process is null.")
            return@withContext
        }

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
            restartProcess()
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
        retryCount = 0
    }

    fun isActive(): Boolean = isRunning.get() && ffmpegProcess != null

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutdown hook triggered. Stopping virtual camera...")
            stop()
        })
    }
}

//package com.ricsdev.ucam.util
//
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import org.slf4j.LoggerFactory
//import java.io.File
//import java.util.concurrent.atomic.AtomicBoolean
//
//class VirtualCamera {
//
//    private val logger = LoggerFactory.getLogger(VirtualCamera::class.java)
//    private var ffmpegProcess: Process? = null
//    private val isRunning = AtomicBoolean(false)
//    private var retryCount = 0
//    private val maxRetries = 3
//    private val retryDelayMs = 500L // Reduced delay
//
//    suspend fun start(devicePath: String = "/dev/video5", cameraMode: String = "Back") = withContext(Dispatchers.IO) {
//        if (devicePath.isBlank() || !File(devicePath).exists()) {
//            logger.error("Invalid device path: $devicePath")
//            throw IllegalArgumentException("Invalid device path: $devicePath")
//        }
//
//        if (!File(devicePath).exists()) {
//            logger.info("Loading v4l2loopback module...")
//            Runtime.getRuntime().exec(arrayOf("sudo", "modprobe", "v4l2loopback"))
//            Thread.sleep(1000)
//        }
//
//        val transposeFilter = if (cameraMode == "Back") "transpose=2" else "transpose=3"
//        val vflipFilter = if (cameraMode == "Back") ",vflip" else ""
//
//        val commandList = listOf(
//            "ffmpeg",
//            "-f", "image2pipe",
//            "-framerate", "30",
//            "-i", "pipe:0",
//            "-vf", "$transposeFilter$vflipFilter,format=yuv420p",
//            "-preset", "ultrafast",
//            "-f", "v4l2",
//            devicePath
//        )
//
//        ffmpegProcess = ProcessBuilder(commandList)
//            .redirectErrorStream(true)
//            .start()
//
//        isRunning.set(true)
//        logger.info("FFmpeg process started successfully.")
//    }
//
//    suspend fun changeOrientation(cameraOrientation: String) = withContext(Dispatchers.IO) {
//        if (!isRunning.get() || ffmpegProcess == null) {
//            logger.warn("Virtual camera is not running or FFmpeg process is null.")
//            return@withContext
//        }
//
//        val transposeFilter = if (cameraOrientation == "Back") "transpose=2" else "transpose=2"
//        val vflipFilter = if (cameraOrientation == "Back") ",vflip" else ",vflip"
//
//        val command = "ffmpeg -vf $transposeFilter$vflipFilter"
//        try {
//            ffmpegProcess?.outputStream?.write(command.toByteArray())
//            ffmpegProcess?.outputStream?.flush()
//            logger.debug("Orientation changed to $cameraOrientation successfully.")
//        } catch (e: Exception) {
//            logger.error("Error changing orientation: ${e.message}", e)
//            restartProcess()
//        }
//    }
//
//    private suspend fun restartProcess() = withContext(Dispatchers.IO) {
//        if (retryCount < maxRetries) {
//            retryCount++
//            logger.info("Attempting to restart FFmpeg process (Retry $retryCount of $maxRetries)...")
//            stop()
//            Thread.sleep(retryDelayMs)
//            start()
//        } else {
//            logger.error("Max retries ($maxRetries) reached. Stopping virtual camera.")
//            stop()
//        }
//    }
//
//    suspend fun writeFrame(frameBytes: ByteArray) = withContext(Dispatchers.IO) {
//        if (!isRunning.get() || ffmpegProcess == null) {
//            logger.warn("Virtual camera is not running or FFmpeg process is null.")
//            return@withContext
//        }
//
//        if (frameBytes.isEmpty()) {
//            logger.warn("Empty frame bytes provided. Skipping write operation.")
//            return@withContext
//        }
//
//        try {
//            ffmpegProcess?.outputStream?.write(frameBytes)
//            ffmpegProcess?.outputStream?.flush()
//            logger.debug("Frame written to virtual camera successfully.")
//        } catch (e: Exception) {
//            logger.error("Error writing frame to virtual camera: ${e.message}", e)
//            restartProcess()
//        }
//    }
//
//    fun stop() {
//        isRunning.set(false)
//        ffmpegProcess?.apply {
//            try {
//                outputStream.close()
//                destroy()
//                logger.info("FFmpeg process stopped successfully.")
//            } catch (e: Exception) {
//                logger.error("Error stopping FFmpeg process: ${e.message}", e)
//            }
//        }
//        ffmpegProcess = null
//        retryCount = 0
//    }
//
//    fun isActive(): Boolean = isRunning.get() && ffmpegProcess != null
//
//    init {
//        Runtime.getRuntime().addShutdownHook(Thread {
//            logger.info("Shutdown hook triggered. Stopping virtual camera...")
//            stop()
//        })
//    }
//}