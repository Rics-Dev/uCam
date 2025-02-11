package com.ricsdev.uconnect.presentation.setupScreen

import androidx.lifecycle.ViewModel
import com.ricsdev.uconnect.util.AppLogger
import com.ricsdev.uconnect.util.VirtualCamera
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import com.ricsdev.uconnect.util.ConnectionManager
import com.ricsdev.uconnect.util.ConnectionStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PairingViewModel(
    private val serverConnection: ConnectionManager,
    connectionStateHolder: ConnectionStateHolder,
    private val virtualCamera: VirtualCamera,
    private val logger: AppLogger
) : ViewModel() {
    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    val currentFrame = _currentFrame.asStateFlow()

    val connectionState = connectionStateHolder.connectionState
    val cameraMode = serverConnection.cameraMode
    val cameraRotation = serverConnection.cameraRotation
    val flipHorizontal = serverConnection.flipHorizontal
    val flipVertical = serverConnection.flipVertical



    private fun startVirtualCamera(){
        viewModelScope.launch {
            try {
                virtualCamera.start(
                    cameraMode = cameraMode.value,
                    flipHorizontal = flipHorizontal.value,
                    flipVertical = flipVertical.value,
                    rotation = cameraRotation.value
                )
            } catch (e: Exception) {
                logger.e("PairingViewModel", "Failed to start virtual camera: ${e.message}")
            }
        }
    }


    private fun collectCameraFrames() {
        viewModelScope.launch {
            serverConnection.cameraFrames.collect { frameBytes ->
                try {
                    _currentFrame.value = convertJPEGToImageBitmap(frameBytes)
                    if (virtualCamera.isActive()) {
                        virtualCamera.writeFrame(frameBytes)
                    }
                } catch (e: Exception) {
                    logger.e("PairingViewModel", "Error processing frame: ${e.message}")
                }
            }
        }
    }


    fun updateCameraOrientation() {
        viewModelScope.launch {
            virtualCamera.changeOrientation(
                cameraOrientation = cameraMode.value,
                flipHorizontal = flipHorizontal.value,
                flipVertical = flipVertical.value,
                rotation = cameraRotation.value
            )
        }
    }

    fun getServerUrl(): String = serverConnection.getServerUrl()

    private fun convertJPEGToImageBitmap(jpegBytes: ByteArray): ImageBitmap {
        val image = Image.makeFromEncoded(jpegBytes)
        return image.toComposeImageBitmap()
    }

    override fun onCleared() {
        super.onCleared()
        virtualCamera.stop()
    }
}