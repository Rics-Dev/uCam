package com.ricsdev.uconnect.util

import kotlinx.coroutines.flow.Flow

actual class NsdHelper {
    actual fun startDiscovery(serviceType: String) {
    }

    actual fun stopDiscovery() {
    }

    actual fun registerService(port: Int) {
    }

    actual val discoveredServers: Flow<String>
        get() = TODO("Not yet implemented")
}