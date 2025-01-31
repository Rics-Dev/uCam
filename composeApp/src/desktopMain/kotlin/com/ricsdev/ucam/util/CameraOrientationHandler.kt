package com.ricsdev.ucam.util

class CameraOrientationHandler {
    data class OrientationConfig(
        val rotationDegrees: Float = 0f,
        val flipHorizontal: Boolean = false,
        val flipVertical: Boolean = false
    )

    fun getDesktopOrientation(
        cameraMode: String,
        userRotation: Float = 0f,
        isFlippedHorizontal: Boolean = false,
        isFlippedVertical: Boolean = false
    ): OrientationConfig {
        // Base rotation to correct Android camera sensor orientation
        val baseRotation = if (cameraMode == "Back") 270f else 90f

        // Combine base rotation with user rotation
        val finalRotation = (baseRotation + userRotation) % 360f

        // For front camera, we typically want horizontal flip by default for mirror effect
        val shouldFlipHorizontal = isFlippedHorizontal != (cameraMode == "Front")

        return OrientationConfig(
            rotationDegrees = finalRotation,
            flipHorizontal = shouldFlipHorizontal,
            flipVertical = isFlippedVertical
        )
    }

    fun getAndroidPreviewOrientation(
        cameraMode: String,
        deviceOrientation: Int
    ): OrientationConfig {
        // Handle Android preview orientation based on device orientation
        val rotation = when (deviceOrientation) {
            90 -> 270f
            270 -> 90f
            180 -> 180f
            else -> 0f
        }

        return OrientationConfig(
            rotationDegrees = rotation,
            // Front camera should be mirrored by default
            flipHorizontal = cameraMode == "Front"
        )
    }
}