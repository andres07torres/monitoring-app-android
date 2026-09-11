package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.data.network.WebSocketManager
import com.example.inframonitor.data.network.WebSocketState
import com.example.inframonitor.ui.UiState
import com.example.inframonitor.utils.NetworkErrorMapper
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

    init {
        observeWebSocketMessages()
    }

    private fun observeWebSocketMessages() {
        wsJob = viewModelScope.launch {
            wsManager.messages.collect { message ->
                currentLogsList.add(message)
                _uiState.value = UiState.Success(currentLogsList.toList())
            }
        }
    }

    fun startLiveLogsWebSocket(containerId: String) {
        wsManager.disconnect()
        currentLogsList.clear()
        _uiState.value = UiState.Loading
        
        // Primero cargamos logs iniciales por REST
        fetchLogs(containerId)
        
        // Conexión exacta al endpoint WebSocket de streaming de logs
        // Ruta final: wss://api-infra.andr3sdev7.com/ws/v1/containers/{container_id}/logs/stream
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
