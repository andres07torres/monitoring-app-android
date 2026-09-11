package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.network.CommandRequest
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.data.network.WebSocketManager
import com.example.inframonitor.data.network.WebSocketState
import com.example.inframonitor.ui.UiState
import com.example.inframonitor.utils.NetworkErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiagnosticsViewModel : ViewModel() {

    private val _containersState = MutableStateFlow<UiState<List<ContainerResponse>>>(UiState.Loading)
    val containersState: StateFlow<UiState<List<ContainerResponse>>> = _containersState.asStateFlow()

    private val _commandOutput = MutableStateFlow("")
    val commandOutput: StateFlow<String> = _commandOutput.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val wsManager = WebSocketManager(viewModelScope)
    val wsState: StateFlow<WebSocketState> = wsManager.connectionState

    private val _systemDetails = MutableStateFlow<Map<String, String>>(
        mapOf(
            "Sistema Operativo" to "Ubuntu 22.04.3 LTS (Docker Engine)",
            "Túnel Zero Trust" to "Cloudflare Tunnel (HTTPS/WSS)",
            "Arquitectura" to "x86_64",
            "Uptime" to "12 días, 4 horas, 22 min",
            "Puertos Protegidos" to "443 (HTTPS), 8443 (WSS)"
        )
    )
    val systemDetails: StateFlow<Map<String, String>> = _systemDetails.asStateFlow()

    private val _healthStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val healthStatus: StateFlow<Map<String, String>> = _healthStatus.asStateFlow()

    init {
        fetchContainers()
        observeWebSocketMessages()
    }

    private fun observeWebSocketMessages() {
        viewModelScope.launch {
            wsManager.messages.collect { message ->
                _commandOutput.value = _commandOutput.value + "\n" + message
            }
        }
    }

    fun connectTerminalWebSocket() {
        wsManager.connect("api/v1/diagnostics/ws")
    }

    fun sendTerminalMessage(input: String) {
        if (!wsManager.sendMessage(input)) {
            // Si el WebSocket no está activo, ejecuta vía REST
            executeCommand(input)
        }
    }

    fun disconnectTerminalWebSocket() {
        wsManager.disconnect()
    }

    fun fetchContainers() {
        viewModelScope.launch {
            _containersState.value = UiState.Loading
            try {
                val response = RetrofitClient.instance.getContainers()
                if (response.isSuccessful && response.body() != null) {
                    _containersState.value = UiState.Success(response.body()!!)
                } else {
                    _containersState.value = UiState.Error(NetworkErrorMapper.parseResponseError(response))
                }
            } catch (e: Exception) {
                _containersState.value = UiState.Error(NetworkErrorMapper.parseThrowable(e))
            }
        }
    }

    fun executeCommand(command: String) {
        viewModelScope.launch {
            _isExecuting.value = true
            try {
                val response = RetrofitClient.instance.executeCommand(CommandRequest(command))
                if (response.isSuccessful) {
                    _commandOutput.value = response.body()?.output ?: "Sin salida"
                } else {
                    _commandOutput.value = NetworkErrorMapper.parseResponseError(response)
                }
            } catch (e: Exception) {
                _commandOutput.value = NetworkErrorMapper.parseThrowable(e)
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun executeAction(action: String) {
        viewModelScope.launch {
            _isExecuting.value = true
            try {
                val command = when(action) {
                    "docker_prune" -> "docker system prune -f"
                    "restart_orchestrator" -> "sudo systemctl restart inframonitor"
                    "dns_check" -> "nslookup google.com"
                    "speedtest" -> "speedtest-cli --simple"
                    else -> ""
                }
                
                val response = RetrofitClient.instance.executeCommand(CommandRequest(command))
                if (response.isSuccessful) {
                    _actionMessage.value = "Éxito: ${action.replace("_", " ").uppercase()}"
                } else {
                    _actionMessage.value = NetworkErrorMapper.parseResponseError(response)
                }
            } catch (e: Exception) {
                _actionMessage.value = NetworkErrorMapper.parseThrowable(e)
            } finally {
                _isExecuting.value = false
                kotlinx.coroutines.delay(4000)
                _actionMessage.value = null
            }
        }
    }

    fun checkHealth(containerId: String, containerName: String) {
        viewModelScope.launch {
            try {
                // HTTPS seguro por Cloudflare Zero Trust
                val testUrl = "https://$containerName/health" 
                
                val response = RetrofitClient.instance.checkHealth(testUrl)
                val statusText = if (response.isSuccessful) {
                    "OK (${response.body()?.status})"
                } else {
                    NetworkErrorMapper.parseResponseError(response)
                }
                
                val current = _healthStatus.value.toMutableMap()
                current[containerId] = statusText
                _healthStatus.value = current
            } catch (e: Exception) {
                val current = _healthStatus.value.toMutableMap()
                current[containerId] = NetworkErrorMapper.parseThrowable(e)
                _healthStatus.value = current
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.disconnect()
    }
}
