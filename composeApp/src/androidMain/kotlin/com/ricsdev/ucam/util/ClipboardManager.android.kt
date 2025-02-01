package com.ricsdev.ucam.util


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

//clipboardManager android
actual class ClipboardManager(context: Context) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val listeners = CopyOnWriteArrayList<(String?) -> Unit>()

    private val _clipboardContent = MutableStateFlow<String?>(null)
    val clipboardContent = _clipboardContent.asStateFlow()

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val content = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        if (content != _clipboardContent.value) {
            _clipboardContent.value = content
            listeners.forEach { it(content) }
        }
    }

    init {
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
        _clipboardContent.value = getContent()
    }

    actual fun getContent(): String? {
        return clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
    }

    actual fun setContent(content: String) {
        val clip = ClipData.newPlainText("Synced Text", content)
        clipboardManager.setPrimaryClip(clip)
        _clipboardContent.value = content
    }

    fun cleanup() {
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)
        listeners.clear()
    }
}