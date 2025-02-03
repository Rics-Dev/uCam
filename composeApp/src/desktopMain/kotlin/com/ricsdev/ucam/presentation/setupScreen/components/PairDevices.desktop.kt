package com.ricsdev.ucam.presentation.setupScreen.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ricsdev.ucam.presentation.setupScreen.SetupViewModel
import com.ricsdev.ucam.util.ConnectionState
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

//pairing screen desktop
@Composable
actual fun PairQrCode(
    viewModel: SetupViewModel,
    onNext: () -> Unit,
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (connectionState) {
                is ConnectionState.Connected -> "Device paired successfully!"
                is ConnectionState.Connecting -> "Connecting to device..."
                is ConnectionState.Disconnected -> "Scan the QR code below to pair your device"
                is ConnectionState.Error -> "Connection failed"
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        val serverUrl = viewModel.getServerUrl()

        // Show QR code only when not connected
        if (connectionState !is ConnectionState.Connected) {
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
        } else {
            // Show success icon when connected
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Connected",
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (connectionState) {
            is ConnectionState.Connected -> {
                Text(
                    text = "You can now use your paired device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Continue")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Continue",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            is ConnectionState.Connecting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Attempt ${(connectionState as ConnectionState.Connecting).attempt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is ConnectionState.Error -> {
                Text(
                    text = (connectionState as ConnectionState.Error).message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = { /* Add retry logic */ },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
            is ConnectionState.Disconnected -> {
                Text(
                    text = "Make sure your other device's camera is pointing at the QR code",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}