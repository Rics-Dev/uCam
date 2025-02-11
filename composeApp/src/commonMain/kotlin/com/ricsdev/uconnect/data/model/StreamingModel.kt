package com.ricsdev.uconnect.data.model

import kotlinx.serialization.Serializable

// StreamingModel.kt
@Serializable
data class FrameData(
    val frame: String, // Base64 encoded image
    val timestamp: Long,
    val resolution: String
)