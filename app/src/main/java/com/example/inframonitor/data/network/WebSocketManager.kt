package com.example.inframonitor.data.network

import com.example.inframonitor.utils.NetworkErrorMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

sealed class WebSocketState {
    data object Disconnected : WebSocketState()
    data object Connecting : WebSocketState()
    data object Connected : WebSocketState()
    data class Error(val message: String) : WebSocketState()
}

class WebSocketManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient = createOkHttpClient()
    private var currentUrlPath: String? = null
    private var isIntentionallyClosed = false
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 128)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // Ping/Pong Heartbeat cada 25 segundos para mantener viva la conexión detrás de Cloudflare Zero Trust (idle timeout ~100s)
            .pingInterval(25, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // Sin timeout de lectura para WebSocket continuo
            .build()
    }

    fun connect(endpointPath: String) {
        if (currentUrlPath == endpointPath && (_connectionState.value is WebSocketState.Connected || _connectionState.value is WebSocketState.Connecting)) {
            // Ya está activo o conectando a este mismo endpoint, ignoramos reconexión redundante
            return
        }

        isIntentionallyClosed = false
        currentUrlPath = endpointPath
        reconnectJob?.cancel()

        val wsBaseUrl = RetrofitClient.getWsBaseUrl()
        val rawUrl = if (endpointPath.startsWith("ws://") || endpointPath.startsWith("wss://")) {
            endpointPath
        } else {
            val sanitizedPath = if (endpointPath.startsWith("/")) endpointPath.drop(1) else endpointPath
            "$wsBaseUrl$sanitizedPath"
        }

        val apiKey = RetrofitClient.getCurrentApiKey()
        
        // Inyección del token tanto en URL (Query Params) como en Headers para compatibilidad total con FastAPI/ASGI y Cloudflare Zero Trust
        val fullUrl = if (!apiKey.isNullOrBlank()) {
            val separator = if (rawUrl.contains("?")) "&" else "?"
            "$rawUrl${separator}token=$apiKey&api_key=$apiKey&x_api_key=$apiKey"
        } else {
            rawUrl
        }

        _connectionState.value = WebSocketState.Connecting

        val requestBuilder = Request.Builder().url(fullUrl)

        if (!apiKey.isNullOrBlank()) {
            requestBuilder.header("X-API-Key", apiKey)
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        val request = requestBuilder.build()

        val oldSocket = webSocket
        webSocket = client.newWebSocket(request, createWebSocketListener())
        
        // Cancelamos la referencia previa sin que dispare reconexión fantasma
        oldSocket?.cancel()
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (webSocket != this@WebSocketManager.webSocket) return
                _connectionState.value = WebSocketState.Connected
                reconnectAttempt = 0
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (webSocket != this@WebSocketManager.webSocket) return
                scope.launch {
                    _messages.emit(text)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (webSocket != this@WebSocketManager.webSocket) return
                scope.launch {
                    _messages.emit(bytes.utf8())
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (webSocket != this@WebSocketManager.webSocket) return
                webSocket.close(1000, null)
                _connectionState.value = WebSocketState.Disconnected
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (webSocket != this@WebSocketManager.webSocket) return
                _connectionState.value = WebSocketState.Disconnected
                if (!isIntentionallyClosed) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (webSocket != this@WebSocketManager.webSocket) return
                val statusCode = response?.code ?: 0
                val errorMsg = if (response != null) {
                    NetworkErrorMapper.parseResponseError(response)
                } else {
                    NetworkErrorMapper.parseThrowable(t)
                }

                _connectionState.value = WebSocketState.Error(errorMsg)

                if (statusCode == 401 || statusCode == 403) {
                    isIntentionallyClosed = true
                } else if (!isIntentionallyClosed) {
                    scheduleReconnect()
                }
            }
        }
    }

    fun sendMessage(text: String): Boolean {
        return webSocket?.send(text) ?: false
    }

    fun disconnect() {
        isIntentionallyClosed = true
        currentUrlPath = null
        reconnectJob?.cancel()
        webSocket?.close(1000, "Cierre normal por cliente")
        webSocket = null
        _connectionState.value = WebSocketState.Disconnected
    }

    private fun scheduleReconnect() {
        if (isIntentionallyClosed || currentUrlPath == null) return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempt++
            val delayMs = (2000L * (1 shl (reconnectAttempt - 1).coerceAtMost(4))).coerceAtMost(30000L)
            _connectionState.value = WebSocketState.Error("Conexión perdida. Reconectando en ${delayMs / 1000}s...")
            delay(delayMs)
            if (!isIntentionallyClosed) {
                currentUrlPath?.let { connect(it) }
            }
        }
    }
}
