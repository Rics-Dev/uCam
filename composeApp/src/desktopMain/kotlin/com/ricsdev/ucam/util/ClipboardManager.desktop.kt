// ClipboardManager.desktop.kt
package com.ricsdev.ucam.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.FlavorListener
import java.awt.datatransfer.StringSelection
import java.util.concurrent.CopyOnWriteArrayList

actual class ClipboardManager {
    private val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    private val listeners = CopyOnWriteArrayList<(String?) -> Unit>()

    private val _clipboardContent = MutableStateFlow<String?>(null)
    val clipboardContent = _clipboardContent.asStateFlow()

    private val flavorListener = FlavorListener {
        try {
            val content = clipboard.getData(DataFlavor.stringFlavor) as? String
            if (content != _clipboardContent.value) {
                _clipboardContent.value = content
                listeners.forEach { it(content) }
            }
        } catch (e: Exception) {
            println("Error handling clipboard change: ${e.message}")
        }
    }

    init {
        clipboard.addFlavorListener(flavorListener)
        _clipboardContent.value = getContent()
    }

    actual fun getContent(): String? {
        return try {
            clipboard.getData(DataFlavor.stringFlavor) as? String
        } catch (e: Exception) {
            println("Error getting clipboard content: ${e.message}")
            null
        }
    }

    actual fun setContent(content: String) {
        try {
            val selection = StringSelection(content)
            clipboard.setContents(selection, selection)
            _clipboardContent.value = content
        } catch (e: Exception) {
            println("Error setting clipboard content: ${e.message}")
        }
    }

    fun cleanup() {
        clipboard.removeFlavorListener(flavorListener)
        listeners.clear()
    }
}