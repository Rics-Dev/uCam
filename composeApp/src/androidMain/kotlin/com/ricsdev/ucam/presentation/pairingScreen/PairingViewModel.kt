package com.ricsdev.ucam.presentation.pairingScreen

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ricsdev.ucam.util.CameraConfig
import com.ricsdev.ucam.util.ConnectionConfig
import com.ricsdev.ucam.util.ConnectionState
import com.ricsdev.ucam.util.KtorClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PairingViewModel(
    private val ktorClient: KtorClient,
    private val cameraManager: CameraConfig
) : ViewModel() {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _useQrScanner = MutableStateFlow(false)
    val useQrScanner: StateFlow<Boolean> = _useQrScanner.asStateFlow()

    private val _useFrontCamera = MutableStateFlow(false)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera.asStateFlow()

    private val _ipAddress = MutableStateFlow("")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    private val _port = MutableStateFlow(ConnectionConfig.DEFAULT_PORT.toString())
    val port: StateFlow<String> = _port.asStateFlow()

    init {
        viewModelScope.launch {
            ktorClient.connectionState.collect { state ->
                _connectionState.value = state
            }
        }
    }

    fun setUseQrScanner(useQrScanner: Boolean) {
        _useQrScanner.value = useQrScanner
    }

    fun setUseFrontCamera(useFrontCamera: Boolean) {
        _useFrontCamera.value = useFrontCamera
        viewModelScope.launch {
            cameraManager.switchCamera(useFrontCamera)
            ktorClient.sendCameraMode(if (useFrontCamera) "Front" else "Back")
        }
    }

    fun setIpAddress(ipAddress: String) {
        _ipAddress.value = ipAddress
    }

    fun setPort(port: String) {
        _port.value = port
    }

    fun connect(serverUrl: String) {
        viewModelScope.launch {
            ktorClient.connect(serverUrl)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            ktorClient.disconnect()
        }
    }

    fun startCamera(view: PreviewView, useFrontCamera: Boolean, lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            cameraManager.startCamera(view, useFrontCamera, lifecycleOwner) { frameBytes ->
                viewModelScope.launch {
                    ktorClient.sendCameraFrame(frameBytes)
                }
            }
        }
    }

    fun rotateCamera(view: PreviewView, rotationAngle: Float) {
        viewModelScope.launch {
            cameraManager.rotateCamera(view, rotationAngle)
            ktorClient.sendCameraRotation(rotationAngle.toString())
        }
    }

    fun flipCamera(view: PreviewView, direction: String) {
        viewModelScope.launch {
            when (direction) {
                "horizontal" -> cameraManager.flipHorizontal(view)
                "vertical" -> cameraManager.flipVertical(view)
            }
            ktorClient.sendCameraFlip(direction)
        }
    }

    fun stopCamera() {
        viewModelScope.launch {
            cameraManager.stopCamera()
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.stopCamera()
        viewModelScope.launch {
            ktorClient.disconnect()
        }
    }
}