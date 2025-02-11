package com.ricsdev.uconnect.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import java.net.InetAddress
import javax.jmdns.ServiceInfo

actual class NsdHelper {
    private val _discoveredServers = MutableSharedFlow<String>()
    actual val discoveredServers: SharedFlow<String> = _discoveredServers.asSharedFlow()

    private var jmdns: JmDNS? = null
    private val SERVICE_TYPE = "_ucam._tcp.local."
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    actual fun startDiscovery(serviceType: String) {
        try {
            jmdns = JmDNS.create(InetAddress.getLocalHost())

            jmdns?.addServiceListener(SERVICE_TYPE, object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                    println("Service added: ${event.info}")
                    jmdns?.requestServiceInfo(event.type, event.name, true)
                }

                override fun serviceRemoved(event: ServiceEvent) {
                    println("Service removed: ${event.info}")
                }

                override fun serviceResolved(event: ServiceEvent) {
                    val info = event.info
                    val serverUrl = "ws://${info.inetAddresses[0].hostAddress}:${info.port}${ConnectionConfig.WS_PATH}"
                    println("Service resolved: $serverUrl")
                    scope.launch {
                        _discoveredServers.emit(serverUrl)
                    }
                }
            })
        } catch (e: Exception) {
            println("Discovery error: ${e.message}")
        }
    }

    actual fun stopDiscovery() {
        jmdns?.close()
        jmdns = null
    }

    actual fun registerService(port: Int, serviceName: String?) {
        try {
            val serviceInfo = ServiceInfo.create(
                SERVICE_TYPE,
                serviceName ?: "UCam-${System.currentTimeMillis()}",
                port,
                "UCam Service"
            )
            jmdns?.registerService(serviceInfo)
        } catch (e: Exception) {
            println("Service registration error: ${e.message}")
        }
    }
}