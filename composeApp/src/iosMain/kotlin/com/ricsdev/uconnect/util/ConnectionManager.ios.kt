package com.ricsdev.uconnect.util

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

actual class ConnectionManager(
    clipboardManager: ClipboardManager,
    cameraConfig: Any?,
) {
    actual val connectionState: StateFlow<ConnectionState>
        get() = TODO("Not yet implemented")
    actual val cameraMode: StateFlow<String>
        get() = TODO("Not yet implemented")
    actual val cameraRotation: StateFlow<Float>
        get() = TODO("Not yet implemented")
    actual val flipHorizontal: StateFlow<Boolean>
        get() = TODO("Not yet implemented")
    actual val flipVertical: StateFlow<Boolean>
        get() = TODO("Not yet implemented")
    actual val cameraFrames: SharedFlow<ByteArray>
        get() = TODO("Not yet implemented")

    actual suspend fun connect(serverUrl: String) {
    }

    actual suspend fun disconnect() {
    }

    actual suspend fun sendCameraFrame(frameBytes: ByteArray) {
    }

    actual suspend fun sendCameraMode(orientation: String) {
    }

    actual suspend fun sendCameraRotation(rotation: String) {
    }

    actual suspend fun sendCameraFlip(direction: String) {
    }

    actual fun getServerUrl(): String {
        TODO("Not yet implemented")
    }

}