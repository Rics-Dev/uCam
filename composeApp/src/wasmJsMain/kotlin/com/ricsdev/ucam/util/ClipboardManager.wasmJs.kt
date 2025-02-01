package com.ricsdev.ucam.util

actual class ClipboardManager {
    actual fun addListener(listener: (String?) -> Unit) {
    }

    actual fun removeListener(listener: (String?) -> Unit) {
    }

    actual fun getContent(): String? {
        TODO("Not yet implemented")
    }

    actual fun setContent(content: String) {
    }
}