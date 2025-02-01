// ClipboardManager.desktop.kt
package com.ricsdev.ucam.util

import com.ricsdev.ucam.data.model.ClipboardMessage
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.FlavorListener
import java.awt.datatransfer.StringSelection

actual class ClipboardManager {
    private val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    private var clipboardScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var clipboardJob: Job? = null
    private var clipboardListenerActive = false

    private val _clipboardContent = MutableStateFlow<String?>(null)
    actual val clipboardContent = _clipboardContent.asStateFlow()

    private val flavorListener = FlavorListener {
        try {
            val content = clipboard.getData(DataFlavor.stringFlavor) as? String
            if (content != _clipboardContent.value) {
                _clipboardContent.value = content
            }
        } catch (e: Exception) {
        }
    }

    init {
        clipboard.addFlavorListener(flavorListener)
        getContent()?.let { content ->
            _clipboardContent.value = content
        }
    }


    actual fun getContent(): String? {
        return try {
            clipboard.getData(DataFlavor.stringFlavor) as? String
        } catch (e: Exception) {
            null
        }
    }

    actual fun setContent(content: String) {
        try {
            val selection = StringSelection(content)
            clipboard.setContents(selection, selection)
            _clipboardContent.value = content
        } catch (e: Exception) {
        }
    }


    actual fun cleanup() {
        clipboard.removeFlavorListener(flavorListener)
    }


    fun setupClipboardListener(session: WebSocketSession) {
        if (!clipboardListenerActive) {
            clipboardListenerActive = true
            clipboardJob = clipboardScope.launch {
                clipboardContent.collect { content ->
                    try {
                        if (content != null) {
                            val message = ClipboardMessage(clipboardMessage = content)
                            session.send(Frame.Text(Json.encodeToString(ClipboardMessage.serializer(), message)))
                        }
                    } catch (e: Exception) {
                        println("Error sending clipboard content: ${e.message}")
                    }
                }
            }
        }
    }

    actual fun stopClipboardListener() {
        clipboardListenerActive = false
        clipboardJob?.cancel()
        clipboardScope.cancel()
    }

}