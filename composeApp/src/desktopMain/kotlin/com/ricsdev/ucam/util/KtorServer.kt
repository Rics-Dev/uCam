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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.BindException
import java.net.NetworkInterface
import kotlin.time.Duration

class KtorServer(
    private val clipboardManager: ClipboardManager,
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentSession: WebSocketSession? = null

    private var clipboardScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var clipboardJob: Job? = null

    private val _cameraMode = MutableStateFlow("Back")
    val cameraMode = _cameraMode.asStateFlow()

    private val _cameraRotation = MutableStateFlow(0f)
    val cameraRotation = _cameraRotation.asStateFlow()

    private val _flipHorizontal = MutableStateFlow(false)
    val flipHorizontal = _flipHorizontal.asStateFlow()

    private val _flipVertical = MutableStateFlow(true)
    val flipVertical = _flipVertical.asStateFlow()

    private val _cameraFrames = MutableSharedFlow<ByteArray>()
    val cameraFrames = _cameraFrames.asSharedFlow()

    init {
        setupClipboardListener()
    }

    private fun setupClipboardListener() {
        clipboardJob = clipboardScope.launch {
            clipboardManager.clipboardContent.collect { content ->
                if (content != null && currentSession != null) {
                    val message = ClipboardMessage(clipboardMessage = content)
                    currentSession?.send(Frame.Text(Json.encodeToString(ClipboardMessage.serializer(), message)))
                }
            }
        }
    }

    fun getServerUrl(): String {
        val ipAddress = NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress ?: "127.0.0.1"
        return "ws://$ipAddress:${ConnectionConfig.DEFAULT_PORT}${ConnectionConfig.WS_PATH}"
    }

    fun start() {
        val serverUrl = getServerUrl()
        println("Starting server at $serverUrl")

        try {
            server = embeddedServer(Netty, port = ConnectionConfig.DEFAULT_PORT) {
                install(WebSockets) {
                    pingPeriod = Duration.parseOrNull("PT10S") ?: Duration.ZERO
                    maxFrameSize = Long.MAX_VALUE
                    masking = false
                }
                routing {
                    webSocket(ConnectionConfig.WS_PATH) {
                        currentSession = this
                        println("WebSocket connection established")
                        _connectionState.value = ConnectionState.Connected
                        setupClipboardListener()

                        try {
                            for (frame in incoming) {
                                when (frame) {
                                    is Frame.Text -> {
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
                            currentSession = null
                            _connectionState.value = ConnectionState.Disconnected
                        }
                    }
                }
            }.start(wait = false)
        } catch (e: BindException) {
            println("Port ${ConnectionConfig.DEFAULT_PORT} is already in use: ${e.message}")
            _connectionState.value = ConnectionState.Error("Port ${ConnectionConfig.DEFAULT_PORT} is already in use")
        } catch (e: Exception) {
            println("Failed to start server: ${e.message}")
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
        }
    }

    fun stop() {
        clipboardJob?.cancel()
        clipboardScope.cancel()
        currentSession = null
        server?.stop(1000, 2000)
        server = null
        _connectionState.value = ConnectionState.Disconnected
        scope.cancel()

        // Create new scopes for next connection
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        clipboardScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Cleanup clipboard manager
        clipboardManager.cleanup()
    }
}