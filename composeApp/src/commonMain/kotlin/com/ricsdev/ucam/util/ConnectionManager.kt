package com.ricsdev.ucam.util
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

expect class ConnectionManager{
    val connectionState: StateFlow<ConnectionState>
    val cameraMode: StateFlow<String>
    val cameraRotation: StateFlow<Float>
    val flipHorizontal: StateFlow<Boolean>
    val flipVertical: StateFlow<Boolean>
    val cameraFrames: SharedFlow<ByteArray>

    suspend fun connect(serverUrl: String = "")
    suspend fun disconnect()
}
