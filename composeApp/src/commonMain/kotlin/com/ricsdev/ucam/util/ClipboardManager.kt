package com.ricsdev.ucam.util

import kotlinx.coroutines.flow.StateFlow

expect class ClipboardManager {
    val clipboardContent: StateFlow<String?>
    fun getContent(): String?
    fun setContent(content: String)
    fun cleanup()
    fun stopClipboardListener()
}
