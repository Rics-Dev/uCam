package com.ricsdev.ucam.util

expect class ClipboardManager {
//    fun addListener(listener: (String?) -> Unit)
//    fun removeListener(listener: (String?) -> Unit)
    fun getContent(): String?
    fun setContent(content: String)
}