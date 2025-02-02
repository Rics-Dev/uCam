package com.ricsdev.ucam.presentation.setupScreen

import androidx.lifecycle.ViewModel
import com.ricsdev.ucam.util.AppLogger
import com.ricsdev.ucam.util.ConnectionManager
import com.ricsdev.ucam.util.ConnectionState
import com.ricsdev.ucam.util.ConnectionStateHolder
import kotlinx.coroutines.flow.StateFlow

expect class SetupViewModel(
    connectionManager: ConnectionManager,
    connectionStateHolder: ConnectionStateHolder,
    logger: AppLogger
) : ViewModel {
    val connectionState: StateFlow<ConnectionState>
//    val currentFrame: StateFlow<ImageBitmap?>
//    val cameraMode: StateFlow<String>
//    val cameraRotation: StateFlow<Float>
//    val flipHorizontal: StateFlow<Boolean>
//    val flipVertical: StateFlow<Boolean>

//    fun updateCameraOrientation()
//    fun getServerUrl(): String
}
