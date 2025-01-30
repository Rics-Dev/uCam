package com.ricsdev.ucam.presentation.pairingScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import com.ricsdev.ucam.util.ConnectionState
import com.ricsdev.ucam.util.KtorServer
import org.jetbrains.skia.Image
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

import androidx.compose.ui.graphics.ImageBitmap
import com.ricsdev.ucam.util.VirtualCamera

@Composable
actual fun PairingScreen(onPaired: (String) -> Unit) {
    val server = remember { KtorServer() }
//    val server = remember { KtorServerWithWebcam() }
    val connectionState by server.connectionState.collectAsState()
    val messages by server.messages.collectAsState()
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    val virtualCamera = remember { VirtualCamera() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        server.start()
        try {
            virtualCamera.start()
        } catch (e: Exception) {
            println("Failed to start virtual camera: ${e.message}")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            server.stop()
            virtualCamera.stop()
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
                            rotationZ = 270f
                            scaleX = -1f
                        }
                )
            }
        }

        LaunchedEffect(Unit) {
            server.cameraFrames.collect { frameBytes ->
                try {
                    currentFrame = convertJPEGToImageBitmap(frameBytes)

                    if (virtualCamera.isActive()) {
                        virtualCamera.writeFrame(frameBytes)
                    }
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
fun convertJPEGToImageBitmap(jpegBytes: ByteArray): ImageBitmap {
    // Use Skia's Image class to decode the JPEG bytes
    val image = Image.makeFromEncoded(jpegBytes)
    // Convert the Skia Image to Compose's ImageBitmap
    return image.toComposeImageBitmap()
}
