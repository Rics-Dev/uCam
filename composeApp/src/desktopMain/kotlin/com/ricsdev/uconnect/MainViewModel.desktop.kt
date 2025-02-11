package com.ricsdev.uconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ricsdev.uconnect.util.ConnectionConfig
import com.ricsdev.uconnect.util.ConnectionManager
import com.ricsdev.uconnect.util.NsdHelper
import kotlinx.coroutines.launch

actual class MainViewModel (
    private val serverConnection: ConnectionManager,
    private val nsdHelper: NsdHelper,
): ViewModel() {

    init {
        startServer()
    }

    private fun startServer() {
        viewModelScope.launch {
            serverConnection.connect()
            nsdHelper.registerService(
                port = ConnectionConfig.DEFAULT_PORT,
                serviceName = "MyDesktopUCam"
            )
            nsdHelper.startDiscovery("_ucam._tcp")
//            serverConnection.startServerDiscovery()
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