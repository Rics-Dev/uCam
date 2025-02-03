package com.ricsdev.ucam.presentation.setupScreen.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ricsdev.ucam.presentation.setupScreen.SetupViewModel
import qrscanner.CameraLens
import qrscanner.QrScanner
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ricsdev.ucam.util.ConnectionState

//pairing screen android
@RequiresApi(Build.VERSION_CODES.O)
@Composable
actual fun PairQrCode(
    viewModel: SetupViewModel,
    onNext: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

        // QR Scanner UI
        if (connectionState !is ConnectionState.Connected) {
            Box(
                modifier = Modifier
                        .size(250.dp)
                        .clip(shape = RoundedCornerShape(size = 14.dp))
                        .clipToBounds()
                        .border(2.dp, Color.Gray, RoundedCornerShape(size = 14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                QrScanner(
                    modifier = Modifier
                        .clipToBounds()
                        .clip(shape = RoundedCornerShape(size = 14.dp)),
                    onCompletion = { url ->
                        // Only proceed if we're not already trying to connect
                        if (connectionState is ConnectionState.Disconnected) {
                            val parts = url.split(":")
                            if (parts.size == 3) {
                                viewModel.setIpAddress(parts[1].substring(2))
                                viewModel.setPort(parts[2].split("/")[0])
                                viewModel.setConnectionState(ConnectionState.Connecting)
                                viewModel.connect(url, context)
                            }
                        }
                    },
                    cameraLens = CameraLens.Back,
                    flashlightOn = false,
                    openImagePicker = false,
                    imagePickerHandler = {},
                    onFailure = {},
                    customOverlay = {},
                )
            }
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
                    text = "Make sure your camera is pointing at the QR code",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}