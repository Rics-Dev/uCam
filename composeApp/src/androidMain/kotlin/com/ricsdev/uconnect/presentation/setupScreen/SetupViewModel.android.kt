package com.ricsdev.uconnect.presentation.setupScreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ricsdev.uconnect.service.ConnectionService
import com.ricsdev.uconnect.util.AppLogger
import com.ricsdev.uconnect.util.ConnectionConfig
import com.ricsdev.uconnect.util.ConnectionManager
import com.ricsdev.uconnect.util.ConnectionState
import com.ricsdev.uconnect.util.ConnectionStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
actual class SetupViewModel actual constructor(
    private val connectionManager: ConnectionManager,
    private val connectionStateHolder: ConnectionStateHolder,
    private val logger: AppLogger
) : ViewModel(){
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    actual val connectionState = connectionStateHolder.connectionState

    private val _ipAddress = MutableStateFlow("")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    private val _port = MutableStateFlow(ConnectionConfig.DEFAULT_PORT.toString())
    val port: StateFlow<String> = _port.asStateFlow()

    // New state to track discovered servers
    private val _discoveredServers = MutableStateFlow<List<String>>(emptyList())
    val discoveredServers: StateFlow<List<String>> = _discoveredServers.asStateFlow()

    // New state to track the selected server
    private val _selectedServer = MutableStateFlow<String?>(null)
    val selectedServer: StateFlow<String?> = _selectedServer.asStateFlow()

    fun setIpAddress(ipAddress: String) {
        _ipAddress.value = ipAddress
    }

    fun setPort(port: String) {
        _port.value = port
    }

    init {
        // Start server discovery
        viewModelScope.launch {
            connectionManager.getNsdHelper().discoveredServers.collect { serverUrl ->
                // Prevent duplicate entries
                if (!_discoveredServers.value.contains(serverUrl)) {
                    _discoveredServers.update { currentList ->
                        currentList + serverUrl
                    }
                }
            }
        }
        connectionManager.startServerDiscovery()
    }


    fun selectServer(serverUrl: String) {
        _selectedServer.value = serverUrl
        // Parse and set IP and port
        val parts = serverUrl.split(":")
        if (parts.size >= 3) {
            setIpAddress(parts[1].substring(2))
            setPort(parts[2].split("/")[0])
        }
    }

    // Clear selected server
    fun clearSelectedServer() {
        _selectedServer.value = null
    }

    // Add method to set connection state
    fun setConnectionState(state: ConnectionState) {
        connectionStateHolder.updateConnectionState(state)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun connect(context: Context) {
        val serverUrl = _selectedServer.value ?: return

        // Only proceed if we're in Connecting state
        if (connectionState.value !is ConnectionState.Connecting) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context as android.app.Activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
                return
            }
        }

        val serviceIntent = Intent(context, ConnectionService::class.java).apply {
            putExtra("serverUrl", serverUrl)
        }
        context.startForegroundService(serviceIntent)
    }

//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun connect(serverUrl: String, context: Context) {
//        // Only proceed if we're in Connecting state
//        if (connectionState.value !is ConnectionState.Connecting) return
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ContextCompat.checkSelfPermission(
//                    context,
//                    Manifest.permission.POST_NOTIFICATIONS
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                ActivityCompat.requestPermissions(
//                    context as android.app.Activity,
//                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
//                    NOTIFICATION_PERMISSION_CODE
//                )
//                return
//            }
//        }
//
//        val serviceIntent = Intent(context, ConnectionService::class.java).apply {
//            putExtra("serverUrl", serverUrl)
//        }
//        context.startForegroundService(serviceIntent)
//    }
//


}
