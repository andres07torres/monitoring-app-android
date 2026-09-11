package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.ContainerLogsResponse
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.data.network.WebSocketManager
import com.example.inframonitor.data.network.WebSocketState
import com.example.inframonitor.ui.UiState
import com.example.inframonitor.utils.NetworkErrorMapper
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<String>>>(UiState.Success(emptyList()))
    val uiState: StateFlow<UiState<List<String>>> = _uiState.asStateFlow()

    private val wsManager = WebSocketManager(viewModelScope)
    val wsState: StateFlow<WebSocketState> = wsManager.connectionState

    private var wsJob: Job? = null
    private val currentLogsList = mutableListOf<String>()
    private val gson = Gson()

    init {
        observeWebSocketMessages()
    }

    private fun observeWebSocketMessages() {
        wsJob = viewModelScope.launch {
            wsManager.messages.collect { rawMessage ->
                val trimmed = rawMessage.trim()
                
                // Soporte híbrido e inteligente: procesa tanto JSON {"logs": [...]} como Texto Plano (Raw String)
                if (trimmed.startsWith("{")) {
                    try {
                        val parsed = gson.fromJson(trimmed, ContainerLogsResponse::class.java)
                        if (parsed?.logs != null) {
                            currentLogsList.addAll(parsed.logs)
                        } else {
                            addRawTextLines(trimmed)
                        }
                    } catch (e: Exception) {
                        addRawTextLines(trimmed)
                    }
                } else {
                    addRawTextLines(trimmed)
                }

                _uiState.value = UiState.Success(currentLogsList.toList())
            }
        }
    }

    private fun addRawTextLines(text: String) {
        val lines = text.split("\n")
            .map { it.trimEnd() }
            .filter { it.isNotEmpty() }

        if (lines.isNotEmpty()) {
            currentLogsList.addAll(lines)
        } else if (text.isNotBlank()) {
            currentLogsList.add(text)
        }
    }

    fun startLiveLogsWebSocket(containerId: String) {
        wsManager.disconnect()
        currentLogsList.clear()
        _uiState.value = UiState.Loading
        
        // Carga inicial de logs por REST
        fetchLogs(containerId)
        
        // Conexión WebSocket seguro de logs
        // Ruta: wss://api-infra.andr3sdev7.com/ws/v1/containers/{container_id}/logs/stream
        wsManager.connect("ws/v1/containers/$containerId/logs/stream")
    }

    fun stopLiveLogsWebSocket() {
        wsManager.disconnect()
    }

    fun fetchLogs(containerId: String) {
        viewModelScope.launch {
            if (currentLogsList.isEmpty()) {
                _uiState.value = UiState.Loading
            }
            try {
                val response = RetrofitClient.instance.getLogs(containerId)
                if (response.isSuccessful && response.body() != null) {
                    val logs = response.body()!!.logs
                    currentLogsList.clear()
                    currentLogsList.addAll(logs)
                    _uiState.value = UiState.Success(currentLogsList.toList())
                } else {
                    if (currentLogsList.isEmpty()) {
                        _uiState.value = UiState.Error(NetworkErrorMapper.parseResponseError(response))
                    }
                }
            } catch (e: Exception) {
                if (currentLogsList.isEmpty()) {
                    _uiState.value = UiState.Error(NetworkErrorMapper.parseThrowable(e))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.disconnect()
    }
}
