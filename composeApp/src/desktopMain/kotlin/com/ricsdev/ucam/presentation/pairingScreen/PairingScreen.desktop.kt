package com.ricsdev.ucam.presentation.pairingScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.ricsdev.ucam.util.ConnectionState
import com.ricsdev.ucam.util.KtorServer
import qrgenerator.qrkitpainter.PatternType
import qrgenerator.qrkitpainter.QrBallType
import qrgenerator.qrkitpainter.QrFrameType
import qrgenerator.qrkitpainter.QrKitShapes
import qrgenerator.qrkitpainter.QrPixelType
import qrgenerator.qrkitpainter.getSelectedFrameShape
import qrgenerator.qrkitpainter.getSelectedPattern
import qrgenerator.qrkitpainter.getSelectedPixel
import qrgenerator.qrkitpainter.getSelectedQrBall
import qrgenerator.qrkitpainter.rememberQrKitPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlin.math.max
import kotlin.math.min
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

@Composable
actual fun PairingScreen(onPaired: (String) -> Unit) {
    val server = remember { KtorServer() }
    val connectionState by server.connectionState.collectAsState()
    val messages by server.messages.collectAsState()
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        server.start()
    }

    DisposableEffect(Unit) {
        onDispose {
            server.stop()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val serverUrl = server.getServerUrl()
        val painter = rememberQrKitPainter(data = serverUrl) {
            shapes = QrKitShapes(
                ballShape = getSelectedQrBall(QrBallType.SquareQrBall()),
                darkPixelShape = getSelectedPixel(QrPixelType.SquarePixel()),
                frameShape = getSelectedFrameShape(QrFrameType.SquareFrame()),
                codeShape = getSelectedPattern(PatternType.SquarePattern),
            )
        }

        Image(
            painter = painter,
            contentDescription = "Server QR Code",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Server URL: $serverUrl")
        Text("Status: ${connectionState::class.simpleName}")

        if (connectionState is ConnectionState.Connected) {
            currentFrame?.let { imageBitmap ->
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Camera Feed",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .graphicsLayer {
                            rotationZ = 90f
                        }
                )
            }
        }

        LaunchedEffect(server.cameraFrames) {
            server.cameraFrames.collect { frameBytes ->
                try {
                    // Assume YUV420 format (most common for Android cameras)
                    // You'll need to adjust these dimensions based on your actual camera output
                    val width = 640  // Example width
                    val height = 480 // Example height

                    // Convert YUV to RGB
                    val rgbImage = convertYUVToRGB(frameBytes, width, height)

                    // Convert to ImageBitmap using toComposeImageBitmap()
                    currentFrame = rgbImage.toComposeImageBitmap()
                } catch (e: Exception) {
                    println("Error processing frame: ${e.message}")
                }
            }
        }

        LaunchedEffect(connectionState) {
            if (connectionState is ConnectionState.Connected) {
                onPaired(serverUrl)
            }
        }
    }
}


private fun convertYUVToRGB(yuvBytes: ByteArray, width: Int, height: Int): BufferedImage {
    // Create output RGB image
    val rgbImage = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)

    // Get the RGB byte array from the image
    val rgbData = (rgbImage.raster.dataBuffer as DataBufferByte).data

    // YUV420 to RGB conversion
    var yIndex = 0
    val uvIndex = width * height

    for (j in 0 until height) {
        for (i in 0 until width) {
            val y = yuvBytes[yIndex].toInt() and 0xff
            val uvRow = j shr 1
            val uvCol = i shr 1
            val uIndex = uvIndex + (uvRow * width + uvCol) * 2
            val vIndex = uIndex + 1

            var u = 0
            var v = 0

            if (uIndex < yuvBytes.size && vIndex < yuvBytes.size) {
                u = (yuvBytes[uIndex].toInt() and 0xff) - 128
                v = (yuvBytes[vIndex].toInt() and 0xff) - 128
            }

            // YUV to RGB conversion
            var r = y + (1.370705f * v)
            var g = y - (0.698001f * v) - (0.337633f * u)
            var b = y + (1.732446f * u)

            r = min(255f, max(0f, r))
            g = min(255f, max(0f, g))
            b = min(255f, max(0f, b))

            val rgbIndex = (j * width + i) * 3
            rgbData[rgbIndex] = b.toInt().toByte()     // B
            rgbData[rgbIndex + 1] = g.toInt().toByte() // G
            rgbData[rgbIndex + 2] = r.toInt().toByte() // R

            yIndex++
        }
    }

    return rgbImage
}