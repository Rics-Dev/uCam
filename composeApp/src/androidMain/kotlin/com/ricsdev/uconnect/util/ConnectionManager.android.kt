package com.ricsdev.uconnect.util

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.ricsdev.uconnect.data.model.ClipboardMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketDeflateExtension
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.time.Duration

//android
@RequiresApi(Build.VERSION_CODES.O)
actual class ConnectionManager actual constructor(
    private val clipboardManager: ClipboardManager,
    private val nsdHelper: NsdHelper,
//    private val cameraConfig: CameraConfig,
) {

    private var client = createClient()
    private var session: DefaultClientWebSocketSession? = null
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastFrameSentTime = 0L

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



    // specific to client
    private fun createClient() = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = Duration.parse("PT30S")
            maxFrameSize = Long.MAX_VALUE
            extensions {
                install(WebSocketDeflateExtension) {
                    compressionLevel = 6
                    compressIf { frame -> frame is Frame.Text && frame.readText().length > 1024 }
                }
            }
        }
    }

    fun getNsdHelper(): NsdHelper {
        return nsdHelper
    }


    init {
        nsdHelper.startDiscovery("_ucam._tcp")
    }

    fun startServerDiscovery() {
        nsdHelper.startDiscovery("_ucam._tcp")
    }


    @RequiresApi(Build.VERSION_CODES.O)
    actual suspend fun connect(serverUrl: String) {
        try {
            Log.d("NetworkManager", "Attempting to connect to: $serverUrl")

            val wsUrl = if (!serverUrl.startsWith("ws://")) {
                serverUrl.replace("http://", "ws://")
            } else serverUrl

            scope.launch {
                try {
                    client.webSocket(wsUrl) {
                        session = this
                        Log.d("NetworkManager", "WebSocket connection established")
                        _connectionState.value = ConnectionState.Connected
                        clipboardManager.setupClipboardListener(this)

                        try {
                            for (frame in incoming) {
                                when (frame) {
                                    is Frame.Text -> handleTextFrame(frame)
//                                    is Frame.Binary -> handleBinaryFrame()
                                    is Frame.Close -> break
                                    is Frame.Ping -> send(Frame.Pong(frame.data))
                                    else -> { /* Handle other frame types if needed */ }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Error in WebSocket connection", "${e.message}", e)
                            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
                        }
                    }
                } catch (e: Exception) {
                    when (e) {
                        is java.nio.channels.UnresolvedAddressException -> {
                            _connectionState.value = ConnectionState.Error("Cannot connect to host: Host not found or unreachable")
                        }
                        is java.net.ConnectException -> {
                            _connectionState.value = ConnectionState.Error("Cannot connect to server: Connection refused")
                        }
                        else -> {
                            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkManager Failed to start connection attempt", "${e.message}", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
        }
    }

    private fun handleTextFrame(frame: Frame.Text) {
        val text = frame.readText()
        try {
            if (text.startsWith("{\"clipboardMessage\"")) {
                val clipboardData = Json.decodeFromString<ClipboardMessage>(text)
                clipboardManager.setContent(clipboardData.clipboardMessage)
            }
            Log.d("NetworkManager", "Received from server: $text")
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error handling text frame: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Failed to process received message")
        }
    }

//    private fun handleBinaryFrame() {
//        val latency = System.currentTimeMillis() - lastFrameSentTime
//        cameraConfig.updateNetworkLatency(latency)
//    }


    actual suspend fun disconnect() {
        try {
            clipboardManager.stopClipboardListener()
            session?.close()
            session = null
            scope.cancel()
            client.close()
            nsdHelper.stopDiscovery()


            client = createClient()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            _connectionState.value = ConnectionState.Disconnected


            clipboardManager.cleanup()
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error during stop: ${e.message}", e)
        }
    }
    suspend fun sendCameraFrame(frameBytes: ByteArray) {
        lastFrameSentTime = System.currentTimeMillis()
        try {
            withTimeout(100) {
                session?.send(Frame.Binary(fin = true, data = frameBytes))
            }
        } catch (e: TimeoutCancellationException) {
            Log.w("NetworkManager", "Frame send timed out, skipping frame")
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