package com.ricsdev.ucam.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class ConnectionState {
    data object Connected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Disconnected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class ConnectionStateHolder(
    connectionManager: ConnectionManager
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    init {
        // Observe connection manager's state
        connectionManager.connectionState.onEach { state ->
            _connectionState.value = state
        }.launchIn(CoroutineScope(Dispatchers.Main))
    }

    fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }
}