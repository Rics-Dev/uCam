package com.ricsdev.ucam.util

import android.util.Log
import com.ricsdev.ucam.data.model.ClipboardMessage
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

class KtorClient(
    private val clipboardManager: ClipboardManager,
    private val cameraConfig: CameraConfig
) {
    private var client = createClient()
    private fun createClient() = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = kotlin.time.Duration.parse("PT30S")
            maxFrameSize = Long.MAX_VALUE
            extensions {
                install(WebSocketDeflateExtension) {
                    compressionLevel = 6
                    compressIf { frame -> frame is Frame.Text && frame.readText().length > 1024 }
                }
            }
        }
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private var session: DefaultClientWebSocketSession? = null
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastFrameSentTime = 0L

    private var clipboardScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var clipboardJob: Job? = null

    init {
        setupClipboardListener()
    }

    private fun setupClipboardListener() {
        clipboardJob = clipboardScope.launch {
            clipboardManager.clipboardContent.collect { content ->
                if (content != null && session != null) {
                    val message = ClipboardMessage(clipboardMessage = content)
                    session?.send(Frame.Text(Json.encodeToString(ClipboardMessage.serializer(), message)))
                }
            }
        }
    }

    suspend fun disconnect() {
        try {
            clipboardJob?.cancel()
            clipboardScope.cancel()
            session?.close()
            session = null
            scope.cancel()
            client.close()

            // Create new instances for next connection
            client = createClient()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            clipboardScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            _connectionState.value = ConnectionState.Disconnected

            // Cleanup clipboard manager
            clipboardManager.cleanup()
        } catch (e: Exception) {
            Log.e("KtorClient", "Error during disconnect: ${e.message}", e)
        }
    }

    private fun initiateConnection(serverUrl: String) {
        try {
            Log.d("KtorClient", "Attempting to connect to: $serverUrl")

            val wsUrl = if (!serverUrl.startsWith("ws://")) {
                serverUrl.replace("http://", "ws://")
            } else serverUrl

            scope.launch {
                try {
                    client.webSocket(wsUrl) {
                        session = this
                        Log.d("KtorClient", "WebSocket connection established")
                        _connectionState.value = ConnectionState.Connected
                        setupClipboardListener()

                        try {
                            send(Frame.Text("Hello from Android!"))
                            Log.d("KtorClient", "Sent test message")

                            for (frame in incoming) {
                                when (frame) {
                                    is Frame.Text -> {
                                        val text = frame.readText()
                                        if (text.startsWith("{\"clipboardMessage\"")) {
                                            val clipboardData = Json.decodeFromString<ClipboardMessage>(text)
                                            // Update clipboard content through the manager
                                            clipboardManager.setContent(clipboardData.clipboardMessage)
                                        }
                                        Log.d("KtorClient", "Received from server: $text")
                                    }
                                    is Frame.Binary -> {
                                        val latency = System.currentTimeMillis() - lastFrameSentTime
                                        cameraConfig.updateNetworkLatency(latency)
                                    }
                                    is Frame.Close -> {
                                        Log.d("KtorClient", "Received close frame")
                                        break
                                    }
                                    is Frame.Ping -> send(Frame.Pong(frame.data))
                                    else -> { /* Handle other frame types if needed */ }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("KtorClient", "Error in WebSocket connection: ${e.message}", e)
                            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
                        }
                    }
                } catch (e: java.nio.channels.UnresolvedAddressException) {
                    Log.e("KtorClient", "Failed to resolve host address: ${e.message}")
                    _connectionState.value = ConnectionState.Error("Cannot connect to host: Host not found or unreachable")
                } catch (e: java.net.ConnectException) {
                    Log.e("KtorClient", "Connection refused: ${e.message}")
                    _connectionState.value = ConnectionState.Error("Cannot connect to server: Connection refused")
                } catch (e: Exception) {
                    Log.e("KtorClient", "Failed to establish WebSocket connection: ${e.message}", e)
                    _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
                } finally {
                    Log.d("KtorClient", "WebSocket connection closed")
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        } catch (e: Exception) {
            Log.e("KtorClient", "Failed to start connection attempt: ${e.message}", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun sendCameraFrame(frameBytes: ByteArray) {
        lastFrameSentTime = System.currentTimeMillis()
        try {
            withTimeout(100) { // Add timeout to prevent blocking
                session?.send(Frame.Binary(fin = true, data = frameBytes))
            }
        } catch (e: TimeoutCancellationException) {
            Log.w("KtorClient", "Frame send timed out, skipping frame")
        }
    }

    suspend fun sendCameraMode(orientation: String) {
        session?.send(Frame.Text("CameraMode:$orientation"))
    }

    suspend fun sendCameraRotation(rotation: String) {
        session?.send(Frame.Text("CameraRotation:$rotation"))
    }

    suspend fun sendCameraFlip(direction: String) {
        session?.send(Frame.Text("CameraFlip:$direction"))
    }
}