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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ricsdev.ucam.util.VirtualCamera
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun PairingScreen(onPaired: (String) -> Unit) {
    val viewModel: PairingViewModel = koinViewModel()

    val connectionState by viewModel.connectionState.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()
    val cameraMode by viewModel.cameraMode.collectAsState()
    val cameraRotation by viewModel.cameraRotation.collectAsState()
    val flipHorizontal by viewModel.flipHorizontal.collectAsState()
    val flipVertical by viewModel.flipVertical.collectAsState()

    LaunchedEffect(cameraMode, flipHorizontal, flipVertical, cameraRotation) {
        viewModel.updateCameraOrientation()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val serverUrl = viewModel.getServerUrl()
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
                        .height(500.dp)
                        .graphicsLayer {
                            rotationZ = if (cameraMode == "Back") cameraRotation + 270f else cameraRotation + 90f
                            scaleX = if (flipVertical) -1f else 1f
                            scaleY = if (flipHorizontal) -1f else 1f
                        }
                )
            }
        }

        LaunchedEffect(connectionState) {
            if (connectionState is ConnectionState.Connected) {
                onPaired(serverUrl)
            }
        }
    }
}