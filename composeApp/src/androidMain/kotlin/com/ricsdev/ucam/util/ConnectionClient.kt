package com.ricsdev.ucam.util


import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class KtorClient {
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = kotlin.time.Duration.parse("PT30S")
            maxFrameSize = Long.MAX_VALUE
        }
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private var session: DefaultClientWebSocketSession? = null

    suspend fun connect(serverUrl: String) {
        try {
            Log.d("KtorClient", "Attempting to connect to: $serverUrl")

            val wsUrl = if (!serverUrl.startsWith("ws://")) {
                serverUrl.replace("http://", "ws://")
            } else serverUrl

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
                } finally {
                    Log.d("KtorClient", "WebSocket connection closed")
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        } catch (e: Exception) {
            Log.e("KtorClient", "Failed to establish WebSocket connection: ${e.message}", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun sendMessage(message: String) {
        session?.send(Frame.Text(message))
    }


    suspend fun sendCameraFrame(frameBytes: ByteArray) {
        session?.send(Frame.Binary(fin = true, data = frameBytes))
    }


    suspend fun sendCameraOrientation(orientation: String) {
        session?.send(Frame.Text("CameraOrientation:$orientation"))
    }


    fun disconnect() {
        client.close()
        _connectionState.value = ConnectionState.Disconnected
    }
}