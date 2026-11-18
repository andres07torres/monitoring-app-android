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
            "Sistema Operativo" to "Cargando...",
            "Túnel Zero Trust" to "Cargando...",
            "Arquitectura" to "Cargando...",
            "Uptime" to "Cargando...",
            "Puertos Protegidos" to "Cargando..."
        )
    )
    val systemDetails: StateFlow<Map<String, String>> = _systemDetails.asStateFlow()

    private val _healthStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val healthStatus: StateFlow<Map<String, String>> = _healthStatus.asStateFlow()

    init {
        fetchContainers()
        fetchSystemDetails()
        observeWebSocketMessages()
    }

    fun fetchSystemDetails() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getSystemStats()
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!
                    _systemDetails.value = mapOf(
                        "Sistema Operativo" to (stats.os ?: "Desconocido"),
                        "Túnel Zero Trust" to (stats.zero_trust_tunnel ?: "Desconocido"),
                        "Arquitectura" to (stats.architecture ?: "Desconocido"),
                        "Uptime" to (stats.uptime ?: "Desconocido"),
                        "Puertos Protegidos" to (stats.protected_ports ?: "Desconocido")
                    )
                } else {
                    _systemDetails.value = mapOf(
                        "Sistema Operativo" to "Error al cargar",
                        "Túnel Zero Trust" to "Error al cargar",
                        "Arquitectura" to "Error al cargar",
                        "Uptime" to "Error al cargar",
                        "Puertos Protegidos" to "Error al cargar"
                    )
                }
            } catch (e: Exception) {
                _systemDetails.value = mapOf(
                    "Sistema Operativo" to "Error de red",
                    "Túnel Zero Trust" to "Error de red",
                    "Arquitectura" to "Error de red",
                    "Uptime" to "Error de red",
                    "Puertos Protegidos" to "Error de red"
                )
            }
        }
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
                    val containers = response.body()!!
                    _containersState.value = UiState.Success(containers)
                    // No ejecutamos checkHealth automáticamente para dejar los items en estado "Pendiente"
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
            // Marca como comprobando manualmente mientras se realiza la petición
            val loadingMap = _healthStatus.value.toMutableMap()
            loadingMap[containerId] = "Comprobando..."
            _healthStatus.value = loadingMap

            try {
                val response = RetrofitClient.instance.checkHealth(containerName)
                val statusText = if (response.isSuccessful && response.body() != null) {
                    val code = response.body()!!.status
                    if (code in 200..299) {
                        "OK ($code)"
                    } else {
                        "FAIL ($code)"
                    }
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
