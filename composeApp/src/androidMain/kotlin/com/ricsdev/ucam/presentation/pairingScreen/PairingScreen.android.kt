package com.ricsdev.ucam.presentation.pairingScreen

import android.util.Log
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ricsdev.ucam.util.CameraManager
import com.ricsdev.ucam.util.ConnectionConfig
import com.ricsdev.ucam.util.ConnectionState
import com.ricsdev.ucam.util.KtorClient
import kotlinx.coroutines.launch
import qrscanner.CameraLens
import qrscanner.OverlayShape
import qrscanner.QrScanner


@Composable
actual fun PairingScreen(onPaired: (String) -> Unit) {
    val client = remember { KtorClient() }
    val connectionState by client.connectionState.collectAsState()
    val scope = rememberCoroutineScope()
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(ConnectionConfig.DEFAULT_PORT.toString()) }
    var useQrScanner by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val cameraManager = remember { CameraManager(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Camera Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "QR Scanner",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("QR Scanner")
                    }
                    Switch(
                        checked = useQrScanner,
                        onCheckedChange = { useQrScanner = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (!useQrScanner && connectionState is ConnectionState.Connected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Front Camera")
                        }
                        Switch(
                            checked = useFrontCamera,
                            onCheckedChange = {
                                useFrontCamera = it
                                scope.launch {
                                    cameraManager.switchCamera(useFrontCamera)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        if (useQrScanner) {
            QrScanner(
                modifier = Modifier.weight(1f),
                onCompletion = { result ->
                    if (result.startsWith("http")) {
                        val url = result.replace("http://", "ws://")
                        val parts = url.split(":")
                        if (parts.size == 3) {
                            ipAddress = parts[1].substring(2)
                            port = parts[2].split("/")[0]
                        }
                        scope.launch {
                            client.connect(url)
                        }
                    }
                },
                onFailure = { /* Handle error */ },
                flashlightOn = false,
                cameraLens = if (useFrontCamera) CameraLens.Front else CameraLens.Back,
                openImagePicker = false,
                imagePickerHandler = { /* Handle image picker */ },
                overlayShape = OverlayShape.Square,
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = { Text("IP Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                val serverUrl = "ws://$ipAddress:$port${ConnectionConfig.WS_PATH}"
                                scope.launch {
                                    client.connect(serverUrl)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Connect")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    client.disconnect()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Disconnect")
                        }
                    }
                }
            }
        }

        if (connectionState is ConnectionState.Connected) {
            var previewView by remember { mutableStateOf<PreviewView?>(null) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(vertical = 16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        PreviewView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            previewView = this
                        }
                    }
                )
            }

            previewView?.let { view ->
                LaunchedEffect(view, useFrontCamera) {
                    cameraManager.startCamera(
                        view,
                        useFrontCamera
                    ) { frameBytes ->
                        scope.launch {
                            client.sendCameraFrame(frameBytes)
                        }
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                cameraManager.stopCamera()
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (connectionState) {
                is ConnectionState.Connected -> Color.Green.copy(alpha = 0.1f)
                is ConnectionState.Error -> Color.Red.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
            )
        ) {
            Text(
                "Status: ${connectionState::class.simpleName}",
                modifier = Modifier.padding(16.dp),
                color = when (connectionState) {
                    is ConnectionState.Connected -> Color.Green.copy(alpha = 0.8f)
                    is ConnectionState.Error -> Color.Red.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        LaunchedEffect(connectionState) {
            if (connectionState is ConnectionState.Connected) {
                val serverUrl = "ws://$ipAddress:$port${ConnectionConfig.WS_PATH}"
                onPaired(serverUrl)
            } else if (connectionState is ConnectionState.Error) {
                Log.d("PairingScreen", "Error: ${(connectionState as ConnectionState.Error).message}")
            }
        }
    }
}