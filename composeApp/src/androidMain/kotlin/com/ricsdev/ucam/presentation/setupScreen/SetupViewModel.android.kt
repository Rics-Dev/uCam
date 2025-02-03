package com.ricsdev.ucam.presentation.setupScreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.ricsdev.ucam.service.ConnectionService
import com.ricsdev.ucam.util.AppLogger
import com.ricsdev.ucam.util.ConnectionConfig
import com.ricsdev.ucam.util.ConnectionManager
import com.ricsdev.ucam.util.ConnectionState
import com.ricsdev.ucam.util.ConnectionStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun setIpAddress(ipAddress: String) {
        _ipAddress.value = ipAddress
    }

    fun setPort(port: String) {
        _port.value = port
    }


    // Add method to set connection state
    fun setConnectionState(state: ConnectionState) {
        connectionStateHolder.updateConnectionState(state)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun connect(serverUrl: String, context: Context) {
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



}
