package com.ricsdev.ucam.presentation.setupScreen.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ricsdev.ucam.presentation.setupScreen.SetupViewModel
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

@Composable
actual fun PairQrCode (
    viewModel: SetupViewModel,
) {

    val connectionState = viewModel.connectionState.value

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Scan the QR code below to pair your device",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Text("Status: ${connectionState::class.simpleName}")
    }
}