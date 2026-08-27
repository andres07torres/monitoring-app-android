package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.network.CommandRequest
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.ui.UiState
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

    private val _healthStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val healthStatus: StateFlow<Map<String, String>> = _healthStatus.asStateFlow()

    init {
        fetchContainers()
    }

    fun fetchContainers() {
        viewModelScope.launch {
            _containersState.value = UiState.Loading
            try {
                val response = RetrofitClient.instance.getContainers()
                if (response.isSuccessful && response.body() != null) {
                    _containersState.value = UiState.Success(response.body()!!)
                } else {
                    _containersState.value = UiState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _containersState.value = UiState.Error(e.localizedMessage ?: "Error desconocido")
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
                    _commandOutput.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                _commandOutput.value = "Error: ${e.localizedMessage}"
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun checkHealth(containerId: String, containerName: String) {
        viewModelScope.launch {
            try {
                // Intentamos deducir una URL de salud basada en el nombre del contenedor
                // Esto es un ejemplo, el backend debería proveer la URL real o manejar esto.
                val testUrl = "http://$containerName:80/health" 
                
                val response = RetrofitClient.instance.checkHealth(testUrl)
                val statusText = if (response.isSuccessful) {
                    "OK (${response.body()?.status})"
                } else {
                    "Error: ${response.code()}"
                }
                
                val current = _healthStatus.value.toMutableMap()
                current[containerId] = statusText
                _healthStatus.value = current
            } catch (e: Exception) {
                val current = _healthStatus.value.toMutableMap()
                current[containerId] = "Error: ${e.localizedMessage}"
                _healthStatus.value = current
            }
        }
    }
}
