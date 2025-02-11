package com.ricsdev.uconnect.presentation.setupScreen

import androidx.lifecycle.ViewModel
import com.ricsdev.uconnect.util.AppLogger
import com.ricsdev.uconnect.util.ConnectionManager
import com.ricsdev.uconnect.util.ConnectionState
import com.ricsdev.uconnect.util.ConnectionStateHolder

actual class SetupViewModel actual constructor(
    private val connectionManager: ConnectionManager,
    private val connectionStateHolder: ConnectionStateHolder,
    private val logger: AppLogger
) : ViewModel(){

    actual val connectionState = connectionStateHolder.connectionState


    fun getServerUrl(): String = connectionManager.getServerUrl()


    fun setConnectionState(state: ConnectionState) {
        connectionStateHolder.updateConnectionState(state)
    }

}