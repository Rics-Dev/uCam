package com.ricsdev.ucam.presentation.pairingScreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ricsdev.ucam.service.ConnectionService
import com.ricsdev.ucam.util.CameraConfig
import com.ricsdev.ucam.util.ConnectionConfig
import com.ricsdev.ucam.util.ConnectionManager
import com.ricsdev.ucam.util.ConnectionStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class PairingViewModel(
    private val clientConnection: ConnectionManager,
    connectionStateHolder: ConnectionStateHolder,
    private val cameraManager: CameraConfig,
) : ViewModel() {

    val connectionState = connectionStateHolder.connectionState

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }


    private val _useQrScanner = MutableStateFlow(false)
    val useQrScanner: StateFlow<Boolean> = _useQrScanner.asStateFlow()

    private val _useFrontCamera = MutableStateFlow(false)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera.asStateFlow()

    private val _ipAddress = MutableStateFlow("")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    private val _port = MutableStateFlow(ConnectionConfig.DEFAULT_PORT.toString())
    val port: StateFlow<String> = _port.asStateFlow()

    fun setUseQrScanner(useQrScanner: Boolean) {
        _useQrScanner.value = useQrScanner
    }

    fun setUseFrontCamera(useFrontCamera: Boolean) {
        _useFrontCamera.value = useFrontCamera
        viewModelScope.launch {
            cameraManager.switchCamera(useFrontCamera)
            clientConnection.sendCameraMode(if (useFrontCamera) "Front" else "Back")
        }
    }

    fun setIpAddress(ipAddress: String) {
        _ipAddress.value = ipAddress
    }

    fun setPort(port: String) {
        _port.value = port
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun connect(serverUrl: String, context: Context) {
        // Check and request notification permission for Android 13+ before starting the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context as android.app.Activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
                return
            }
        }

        // Start the service if permission is granted or not needed
        val serviceIntent = Intent(context, ConnectionService::class.java).apply {
            putExtra("serverUrl", serverUrl)
        }
        context.startForegroundService(serviceIntent)
    }


//    @RequiresApi(Build.VERSION_CODES.O)
//    fun connect(serverUrl: String, context: Context) {
//        val serviceIntent = Intent(context, ConnectionService::class.java).apply {
//            putExtra("serverUrl", serverUrl)
//        }
//        context.startForegroundService(serviceIntent)
//    }

    fun disconnect(context: Context) {
        context.stopService(Intent(context, ConnectionService::class.java))
        viewModelScope.launch {
            clientConnection.disconnect()
        }
    }

    fun startCamera(view: PreviewView, useFrontCamera: Boolean, lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            cameraManager.startCamera(view, useFrontCamera, lifecycleOwner) { frameBytes ->
                viewModelScope.launch {
                    clientConnection.sendCameraFrame(frameBytes)
                }
            }
        }
    }

    fun rotateCamera(view: PreviewView, rotationAngle: Float) {
        viewModelScope.launch {
            cameraManager.rotateCamera(view, rotationAngle)
            clientConnection.sendCameraRotation(rotationAngle.toString())
        }
    }

    fun flipCamera(view: PreviewView, direction: String) {
        viewModelScope.launch {
            when (direction) {
                "horizontal" -> cameraManager.flipHorizontal(view)
                "vertical" -> cameraManager.flipVertical(view)
            }
            clientConnection.sendCameraFlip(direction)
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
    }
}