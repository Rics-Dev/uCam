package com.ricsdev.ucam.util

import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.NetworkInterface
import kotlin.time.Duration


open class KtorServer {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()
    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _cameraOrientation = MutableStateFlow("Back")
    val cameraOrientation = _cameraOrientation.asStateFlow()

    private val _cameraFrames = MutableSharedFlow<ByteArray>()
    val cameraFrames = _cameraFrames.asSharedFlow()


    fun getServerUrl(): String {
        val ipAddress = NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress ?: "127.0.0.1"
        return "ws://$ipAddress:${ConnectionConfig.DEFAULT_PORT}${ConnectionConfig.WS_PATH}"
    }

    open fun start() {
        val serverUrl = getServerUrl()
        println("Starting server at $serverUrl")




        server = embeddedServer(Netty, port = ConnectionConfig.DEFAULT_PORT) {
            install(WebSockets) {
                pingPeriod = Duration.parseOrNull("PT30S") ?: Duration.ZERO
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            routing {
                webSocket(ConnectionConfig.WS_PATH) {
                    println("WebSocket connection established")
                    _connectionState.value = ConnectionState.Connected

                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    println("Server received: $text")
                                    if (text.startsWith("CameraOrientation:")) {
                                        _cameraOrientation.value = text.removePrefix("CameraOrientation:")
                                    } else {
                                        println("Server received: $text")
                                        _messages.update { it + text }
                                        outgoing.send(Frame.Text("Server received: $text"))
                                    }
                                }
                                is Frame.Binary -> {
                                    val frameBytes = frame.readBytes()
                                    _cameraFrames.emit(frameBytes)

                                }
                                else -> { /* Handle other frame types if needed */ }
                            }
                        }
                    } catch (e: Exception) {
                        println("Error in WebSocket connection: ${e.message}")
                        _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
                    } finally {
                        println("WebSocket connection closed")
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }
            }
        }.start(wait = false)
    }

    open fun stop() {
        server?.stop(1000, 2000)
        server = null
        _messages.value = emptyList()
        _connectionState.value = ConnectionState.Disconnected



    }
}