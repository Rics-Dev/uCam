package com.ricsdev.uconnect.util
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

expect class ConnectionManager(
    clipboardManager: ClipboardManager,
    nsdHelper: NsdHelper
) {
    val connectionState: StateFlow<ConnectionState>
    val cameraMode: StateFlow<String>
    val cameraRotation: StateFlow<Float>
    val flipHorizontal: StateFlow<Boolean>
    val flipVertical: StateFlow<Boolean>
    val cameraFrames: SharedFlow<ByteArray>

    suspend fun connect(serverUrl: String = "")
    suspend fun disconnect()
}
