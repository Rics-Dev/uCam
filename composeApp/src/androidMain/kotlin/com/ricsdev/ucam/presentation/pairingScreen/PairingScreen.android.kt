package com.ricsdev.ucam.presentation.pairingScreen

import android.util.Log
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    var message by remember { mutableStateOf("") }


    val context = LocalContext.current
    val cameraManager = remember { CameraManager(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Use QR Scanner")
            Switch(
                checked = useQrScanner,
                onCheckedChange = { useQrScanner = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                cameraLens = CameraLens.Back,
                openImagePicker = false,
                imagePickerHandler =  { /* Handle image picker */ },
                overlayShape = OverlayShape.Square,
            )
        } else {
            TextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("Enter IP Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Enter Port") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    val serverUrl = "ws://$ipAddress:$port${ConnectionConfig.WS_PATH}"
                    scope.launch {
                        client.connect(serverUrl)
                    }
                }) {
                    Text("Connect")
                }
                Button(onClick = {
                    scope.launch {
                        client.disconnect()
                    }
                }) {
                    Text("Disconnect")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (connectionState is ConnectionState.Connected) {
            var previewView by remember { mutableStateOf<PreviewView?>(null) }

            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        previewView = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            // Move LaunchedEffect outside of AndroidView
            previewView?.let { view ->
                LaunchedEffect(view) {
                    cameraManager.startCamera(view) { frameBytes ->
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

        Text("Status: ${connectionState::class.simpleName}")

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