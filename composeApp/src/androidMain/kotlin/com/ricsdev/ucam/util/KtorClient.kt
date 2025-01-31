package com.ricsdev.ucam.util


import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.net.URI
import java.net.URISyntaxException

class KtorClient(private val cameraManager: CameraManager) {
    private var client = createClient()
    private fun createClient() = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = kotlin.time.Duration.parse("PT30S")
            maxFrameSize = Long.MAX_VALUE
            extensions {
                install(WebSocketDeflateExtension) {
                    compressionLevel = 6 // Balance between compression and CPU usage
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




    suspend fun disconnect() {
        try {
            session?.close()
            session = null
            scope.cancel()
            client.close()
            // Create new instances for next connection
            client = createClient()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            _connectionState.value = ConnectionState.Disconnected
        } catch (e: Exception) {
            Log.e("KtorClient", "Error during disconnect: ${e.message}", e)
        }
    }

    fun connect(serverUrl: String) {
        if (!isValidUrl(serverUrl)) {
            Log.e("KtorClient", "Invalid URL: $serverUrl")
            _connectionState.value = ConnectionState.Error("Invalid URL: $serverUrl")
            return
        }

        if (_connectionState.value is ConnectionState.Connected) {
            Log.d("KtorClient", "Already connected, disconnecting first")
            scope.launch {
                disconnect()
                delay(1000) // Give some time for cleanup
                initiateConnection(serverUrl)
            }
        } else {
            initiateConnection(serverUrl)
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

                        try {
                            send(Frame.Text("Hello from Android!"))
                            Log.d("KtorClient", "Sent test message")

                            for (frame in incoming) {
                                when (frame) {
                                    is Frame.Text -> {
                                        val text = frame.readText()
                                        Log.d("KtorClient", "Received from server: $text")
                                    }
                                    is Frame.Binary -> {
                                        // Calculate round-trip time when receiving frame acknowledgment
                                        val latency = System.currentTimeMillis() - lastFrameSentTime
                                        cameraManager.updateNetworkLatency(latency)
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

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            if (uri.scheme != "ws" && uri.scheme != "wss") {
                Log.e("KtorClient", "Invalid scheme: ${uri.scheme}")
                return false
            }
            if (uri.host == null) {
                Log.e("KtorClient", "No host specified")
                return false
            }
            if (uri.port == -1) {
                Log.e("KtorClient", "No port specified")
                return false
            }
            true
        } catch (e: URISyntaxException) {
            Log.e("KtorClient", "Invalid URL format: ${e.message}")
            false
        }
    }

//    suspend fun sendMessage(message: String) {
//        session?.send(Frame.Text(message))
//    }

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

//    suspend fun sendCameraFrame(frameBytes: ByteArray) {
//        lastFrameSentTime = System.currentTimeMillis()
//        session?.send(Frame.Binary(fin = true, data = frameBytes))
//    }

//    suspend fun sendCameraFrame(frameBytes: ByteArray) {
//        session?.send(Frame.Binary(fin = true, data = frameBytes))
//    }

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