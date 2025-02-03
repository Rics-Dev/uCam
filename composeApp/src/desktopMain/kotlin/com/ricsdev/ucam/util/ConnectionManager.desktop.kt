package com.ricsdev.ucam.util

import com.ricsdev.ucam.data.model.ClipboardMessage
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.net.BindException
import java.net.NetworkInterface
import kotlin.time.Duration

//desktop
actual class ConnectionManager actual constructor(
    private val clipboardManager: ClipboardManager,
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var session: WebSocketSession? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState = _connectionState.asStateFlow()

    private val _cameraMode = MutableStateFlow("Back")
    actual val cameraMode = _cameraMode.asStateFlow()

    private val _cameraRotation = MutableStateFlow(0f)
    actual val cameraRotation = _cameraRotation.asStateFlow()

    private val _flipHorizontal = MutableStateFlow(false)
    actual val flipHorizontal = _flipHorizontal.asStateFlow()

    private val _flipVertical = MutableStateFlow(true)
    actual val flipVertical = _flipVertical.asStateFlow()

    private val _cameraFrames = MutableSharedFlow<ByteArray>()
    actual val cameraFrames = _cameraFrames.asSharedFlow()



    fun getServerUrl(): String {
        return try {
            val ipAddress = NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress ?: throw  Exception("No suitable network interface found")
            "ws://$ipAddress:${ConnectionConfig.DEFAULT_PORT}${ConnectionConfig.WS_PATH}"
        } catch (e: Exception) {
            throw Exception("Failed to determine server URL: ${e.message}")
        }
    }


    actual suspend fun connect(serverUrl: String) {
        println("Starting server at ${getServerUrl()}")

        try {
            server = embeddedServer(Netty, port = ConnectionConfig.DEFAULT_PORT) {
                install(WebSockets) {
                    pingPeriod = Duration.parseOrNull("PT10S") ?: Duration.ZERO
                    maxFrameSize = Long.MAX_VALUE
                    masking = false
                }
                routing {
                    webSocket(ConnectionConfig.WS_PATH) {
                        session = this
                        println("WebSocket connection established")
                        _connectionState.value = ConnectionState.Connected
                        clipboardManager.setupClipboardListener(this)

                        try {
                            for (frame in incoming) {
                                when (frame) {
                                    is Frame.Text -> handleTextFrame(frame)
                                    is Frame.Binary -> handleBinaryFrame(frame)
                                    else -> { /* Handle other frame types if needed */ }
                                }
                            }
                        } catch (e: Exception) {
                            println("Error in WebSocket connection: ${e.message}")
                            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
                        } finally {
                            println("WebSocket connection closed")
                            session = null
                            _connectionState.value = ConnectionState.Disconnected
                        }
                    }
                }
            }.start(wait = false)
        } catch (e: Exception) {
            when (e) {
                is BindException -> {
                    println("Port ${ConnectionConfig.DEFAULT_PORT} is already in use: ${e.message}")
                    _connectionState.value = ConnectionState.Error("Port ${ConnectionConfig.DEFAULT_PORT} is already in use")
                }
                else -> {
                    println("Failed to start server: ${e.message}")
                    _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun handleTextFrame(frame: Frame.Text) {
        val text = frame.readText()
        println("Server received: $text")
        when {
            text.startsWith("{\"clipboardMessage\"") -> {
                val clipboardData = Json.decodeFromString<ClipboardMessage>(text)
                clipboardManager.setContent(clipboardData.clipboardMessage)
            }
            text.startsWith("CameraMode:") -> {
                _cameraMode.value = text.removePrefix("CameraMode:")
            }
            text.startsWith("CameraRotation:") -> {
                _cameraRotation.value = text.removePrefix("CameraRotation:").toFloat()
            }
            text.startsWith("CameraFlip:horizontal") -> {
                _flipHorizontal.value = !_flipHorizontal.value
            }
            text.startsWith("CameraFlip:vertical") -> {
                _flipVertical.value = !_flipVertical.value
            }
        }
    }

    private suspend fun handleBinaryFrame(frame: Frame.Binary) {
        val frameBytes = frame.readBytes()
        _cameraFrames.emit(frameBytes)
    }




    actual suspend fun disconnect() {
        clipboardManager.stopClipboardListener()
        session = null
        server?.stop(1000, 2000)
        server = null
        _connectionState.value = ConnectionState.Disconnected
        scope.cancel()


        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        clipboardManager.cleanup()
    }
}