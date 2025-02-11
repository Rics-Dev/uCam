package com.ricsdev.uconnect.util

import kotlinx.coroutines.flow.SharedFlow

expect class NsdHelper {
    fun startDiscovery(serviceType: String)
    fun stopDiscovery()
    fun registerService(port: Int, serviceName: String? = null)
    val discoveredServers: SharedFlow<String> // Emits server URLs as they are discovered
}