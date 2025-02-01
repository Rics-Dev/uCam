package com.ricsdev.ucam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ricsdev.ucam.util.ConnectionManager
import kotlinx.coroutines.launch

actual class MainViewModel (
    private val serverConnection: ConnectionManager,
): ViewModel() {

    init {
        startServer()
    }

    private fun startServer() {
        viewModelScope.launch {
            serverConnection.connect()
            try {
//                virtualCamera.start(
//                    cameraMode = cameraMode.value,
//                    flipHorizontal = flipHorizontal.value,
//                    flipVertical = flipVertical.value,
//                    rotation = cameraRotation.value
//                )
            } catch (e: Exception) {
                println("PairingViewModel Failed to start virtual camera: ${e.message}")
            }
        }
    }




    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            serverConnection.disconnect()
        }
    }
}