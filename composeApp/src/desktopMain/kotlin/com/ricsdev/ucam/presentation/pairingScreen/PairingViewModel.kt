package com.ricsdev.ucam.presentation.pairingScreen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.lifecycle.ViewModel
import com.ricsdev.ucam.util.AppLogger
import com.ricsdev.ucam.util.KtorServer
import com.ricsdev.ucam.util.VirtualCamera
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PairingViewModel(
    private val server: KtorServer,
    private val virtualCamera: VirtualCamera,
    private val logger: AppLogger
) : ViewModel() {
    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    val currentFrame = _currentFrame.asStateFlow()

    val connectionState = server.connectionState
    val cameraMode = server.cameraMode
    val cameraRotation = server.cameraRotation
    val flipHorizontal = server.flipHorizontal
    val flipVertical = server.flipVertical

    init {
        startServer()
        collectCameraFrames()
    }

    private fun startServer() {
        viewModelScope.launch {
            server.start()
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
            server.cameraFrames.collect { frameBytes ->
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

    fun getServerUrl(): String = server.getServerUrl()

    private fun convertJPEGToImageBitmap(jpegBytes: ByteArray): ImageBitmap {
        val image = Image.makeFromEncoded(jpegBytes)
        return image.toComposeImageBitmap()
    }

    override fun onCleared() {
        super.onCleared()
        server.stop()
        virtualCamera.stop()
    }
}