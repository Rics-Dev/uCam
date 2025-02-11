package com.ricsdev.uconnect.util

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

actual class NsdHelper(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _discoveredServers = MutableSharedFlow<String>(replay = 1)
    actual val discoveredServers: SharedFlow<String> = _discoveredServers.asSharedFlow()

    private var isDiscovering = false
    private var serviceRegistration: NsdServiceInfo? = null

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Log.e("NsdHelper", "Discovery start failed: $errorCode")
            isDiscovering = false
            stopDiscovery()
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Log.e("NsdHelper", "Discovery stop failed: $errorCode")
            isDiscovering = false
            stopDiscovery()
        }

        override fun onDiscoveryStarted(serviceType: String?) {
            Log.d("NsdHelper", "Service discovery started for type: $serviceType")
            isDiscovering = true
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            Log.d("NsdHelper", "Service discovery stopped")
            isDiscovering = false
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            Log.d("NsdHelper", "Service lost: ${serviceInfo?.serviceName}")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d("NsdHelper", "Service found: ${service.serviceName}")
            if (service.serviceType == "_ucam._tcp.local.") {
                Log.d("NsdHelper", "Resolving service: ${service.serviceName}")
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                        Log.e("NsdHelper", "Resolve failed: $errorCode")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val serverUrl = "ws://${serviceInfo.host.hostAddress}:${serviceInfo.port}${ConnectionConfig.WS_PATH}"
                        Log.d("NsdHelper", "Resolved service: $serverUrl")
                        CoroutineScope(Dispatchers.IO).launch {
                            _discoveredServers.emit(serverUrl)
                        }
                    }
                })
            }
        }
    }

    actual fun startDiscovery(serviceType: String) {
        if (!isDiscovering) {
            try {
                // Remove the dot concatenation
                nsdManager.discoverServices(
                    serviceType,  // Don't add extra dot
                    NsdManager.PROTOCOL_DNS_SD,
                    discoveryListener
                )
            } catch (e: Exception) {
                Log.e("NsdHelper", "Failed to start discovery", e)
            }
        }
    }

    actual fun stopDiscovery() {
        if (isDiscovering) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Log.e("NsdHelper", "Failed to stop discovery", e)
            }
            isDiscovering = false
        }
    }

    actual fun registerService(port: Int, serviceName: String?) {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName ?: "UCam-${System.currentTimeMillis()}"
            serviceType = "_ucam._tcp."
            setPort(port)
        }

        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.e("NsdHelper", "Service registration failed: $errorCode")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.e("NsdHelper", "Service unregistration failed: $errorCode")
                }

                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    Log.d("NsdHelper", "Service registered: ${serviceInfo.serviceName}")
                    this@NsdHelper.serviceRegistration = serviceInfo
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                    Log.d("NsdHelper", "Service unregistered")
                }
            }
        )
    }
}