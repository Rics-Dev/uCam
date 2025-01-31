package com.ricsdev.ucam.util

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
    private val frameBuffer = LinkedBlockingQueue<ByteArray>(30) // Buffer up to 30 frames
    private val maxBufferSize = 30 // Maximum number of frames to buffer
    private val frameSkippingThreshold = 20 // Skip frames if buffer exceeds this size

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
                    } catch (e: Exception) {
                        logger.e("VirtualCamera", "Error writing frame to virtual camera: ${e.message}")
                        restartProcess()
                    }
                }
            }
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




//package com.ricsdev.ucam.util
//
//import kotlinx.coroutines.withContext
//import kotlinx.coroutines.Dispatchers
//import java.io.File
//import java.util.concurrent.atomic.AtomicBoolean
//
//private val logger = AppLogger
//
//class VirtualCamera {
//
//    private var ffmpegProcess: Process? = null
//    private val isRunning = AtomicBoolean(false)
//    private var retryCount = 0
//    private val maxRetries = 3
//    private val retryDelayMs = 500L // Reduced delay
//
//
//    suspend fun start(
//        devicePath: String = "/dev/video5",
//        cameraMode: String = "Back",
//        flipHorizontal: Boolean = false,
//        flipVertical: Boolean = false,
//        rotation: Float = 0f
//    ) = withContext(Dispatchers.IO) {
//
//        if (!File(devicePath).exists()) {
//            logger.i("VirtualCamera", "Loading v4l2loopback module...")
//            Runtime.getRuntime().exec(arrayOf(
//                "pkexec",
//                "modprobe",
//                "v4l2loopback",
//                "devices=1",
//                "video_nr=5",
//                "card_label=uConnect Virtual Camera",
//                "exclusive_caps=1"
//            ))
//            Thread.sleep(1000)
//        }
//
//        val transposeFilter = if (cameraMode == "Back") "transpose=0" else "transpose=3"
//        val hflipFilter = if (flipHorizontal) ",hflip" else ""
//        val vflipFilter = if (!flipVertical) ",vflip" else "" // Inverted logic
//        val rotateFilter = if (rotation != 0f) ",rotate=${rotation}*PI/180" else ""
//
//        val commandList = listOf(
//            "ffmpeg",
//            "-f", "image2pipe",
//            "-framerate", "30",
//            "-i", "pipe:0",
//            "-vf", "$transposeFilter$hflipFilter$vflipFilter$rotateFilter,format=yuv420p",
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
//        logger.i("VirtualCamera", "FFmpeg process started successfully.")
//
//    }
//
//    suspend fun changeOrientation(
//        cameraOrientation: String,
//        flipHorizontal: Boolean = false,
//        flipVertical: Boolean = false,
//        rotation: Float = 0f
//    ) = withContext(Dispatchers.IO) {
//        if (!isRunning.get() || ffmpegProcess == null) {
//            logger.e("VirtualCamera", "Virtual camera is not running or FFmpeg process is null.")
//            return@withContext
//        }
//        stop() // Stop the current FFmpeg process
//
//        // Restart the FFmpeg process with the new parameters
//        start(
//            cameraMode = cameraOrientation,
//            flipHorizontal = flipHorizontal,
//            flipVertical = flipVertical,
//            rotation = rotation
//        )
//    }
//
//    private suspend fun restartProcess() = withContext(Dispatchers.IO) {
//        if (retryCount < maxRetries) {
//            retryCount++
//            logger.i("VirtualCamera", "Attempting to restart FFmpeg process (Retry $retryCount of $maxRetries)...")
//            stop()
//            Thread.sleep(retryDelayMs)
//            start()
//        } else {
//            logger.e("VirtualCamera", "Max retries ($maxRetries) reached. Stopping virtual camera.")
//            stop()
//        }
//    }
//
//    suspend fun writeFrame(frameBytes: ByteArray) = withContext(Dispatchers.IO) {
//        if (!isRunning.get() || ffmpegProcess == null) {
//            logger.e("VirtualCamera", "Virtual camera is not running or FFmpeg process is null.")
//            return@withContext
//        }
//
//        if (frameBytes.isEmpty()) {
//            logger.e("VirtualCamera", "Empty frame bytes provided. Skipping write operation.")
//            return@withContext
//        }
//
//        try {
//            ffmpegProcess?.outputStream?.write(frameBytes)
//            ffmpegProcess?.outputStream?.flush()
//        } catch (e: Exception) {
//            logger.e("VirtualCamera", "Error writing frame to virtual camera: ${e.message}")
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
//                logger.i("VirtualCamera", "FFmpeg process stopped successfully.")
//            } catch (e: Exception) {
//                logger.e("VirtualCamera", "Error stopping FFmpeg process: ${e.message}")
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
//            logger.i("VirtualCamera", "Shutdown hook triggered. Stopping virtual camera...")
//            stop()
//        })
//    }
//}