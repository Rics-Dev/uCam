package com.ricsdev.ucam.util


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.ricsdev.ucam.data.model.ClipboardMessage
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

//clipboardManager android
actual class ClipboardManager(context: Context) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var clipboardScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var clipboardJob: Job? = null
    private var clipboardListenerActive = false
    private var isInitialized = false

    private val _clipboardContent = MutableStateFlow<String?>(null)
    actual val clipboardContent = _clipboardContent.asStateFlow()


    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val content = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        if (content != _clipboardContent.value) {
            _clipboardContent.value = content
        }
    }

    init {
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
        getContent()?.let { content ->
            _clipboardContent.value = content
        }
    }

    actual fun getContent(): String? {
        return try {
            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        } catch (e: Exception) {
            null
        }
    }

    actual fun setContent(content: String) {
        try {
            val clip = ClipData.newPlainText("Synced Text", content)
            clipboardManager.setPrimaryClip(clip)
            _clipboardContent.value = content
        } catch (e: Exception) {
            null
        }
    }


    actual fun cleanup() {
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)
    }


    fun setupClipboardListener(session: DefaultClientWebSocketSession) {
        if (!clipboardListenerActive) {
            clipboardListenerActive = true
            clipboardJob = clipboardScope.launch {
                clipboardContent.collect { content ->
                    try {
                        if (content != null && isInitialized) {
                            val message = ClipboardMessage(clipboardMessage = content)
                            session.send(Frame.Text(Json.encodeToString(ClipboardMessage.serializer(), message)))
                        }else{
                            isInitialized = true
                        }
                    } catch (e: Exception) {
                        Log.e("ClipboardManager", "Error sending clipboard content: ${e.message}", e)
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