package com.ricsdev.ucam.util

sealed class ConnectionState {
    data object Connected : ConnectionState()
    data object Disconnected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class ConnectionStateHolder(
    connectionManager: ConnectionManager
) {
    val connectionState = connectionManager.connectionState
}