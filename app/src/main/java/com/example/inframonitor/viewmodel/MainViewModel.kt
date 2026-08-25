package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.AlertResponse
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.models.LogResponse
import com.example.inframonitor.data.models.SystemStatsResponse
import com.example.inframonitor.data.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _systemStats = MutableStateFlow<SystemStatsResponse?>(
        SystemStatsResponse(62.0, 45.0, 7.2, 16.0) // Mock inicial
    )
    val systemStats: StateFlow<SystemStatsResponse?> = _systemStats.asStateFlow()

    private val _containers = MutableStateFlow<List<ContainerResponse>>(
        listOf(
            ContainerResponse("1", "nginx-proxy", "running"),
            ContainerResponse("2", "postgres-db", "running"),
            ContainerResponse("3", "plex-media", "restarting"),
            ContainerResponse("4", "grafana", "exited"),
            ContainerResponse("5", "watchtower", "exited")
        )
    )
    val containers: StateFlow<List<ContainerResponse>> = _containers.asStateFlow()

    private val _logs = MutableStateFlow<List<LogResponse>>(
        listOf(
            LogResponse("14:22:01", "INFO", "nginx", "GET /api/v1/status 200 - 8ms"),
            LogResponse("14:22:03", "INFO", "nginx", "GET /metrics 200 - 4ms"),
            LogResponse("14:22:05", "WARN", "nginx", "upstream plex-media respondió lento (612ms)")
        )
    )
    val logs: StateFlow<List<LogResponse>> = _logs.asStateFlow()

    private val _alerts = MutableStateFlow<List<AlertResponse>>(
        listOf(
            AlertResponse("1", "grafana caído", "El contenedor salió con código 1", "hace 2 min", "rack-01.lan", "critical"),
            AlertResponse("2", "plex-media se reinició", "Uso de memoria al 92%", "hace 6 min", "rack-01.lan", "warning")
        )
    )
    val alerts: StateFlow<List<AlertResponse>> = _alerts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Intentamos cargar datos reales, pero si falla, los mocks ya están ahí
        fetchInitialData()
        
        viewModelScope.launch {
            while (true) {
                delay(5000)
                updateDynamicData()
            }
        }
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            try {
                fetchContainers()
                fetchAlerts()
                fetchLogs()
                updateStats()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun updateDynamicData() {
        try {
            updateStats()
            fetchAlerts()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchContainers() {
        val response = RetrofitClient.instance.getContainers()
        if (response.isSuccessful) {
            _containers.value = response.body() ?: _containers.value
        }
    }

    private suspend fun fetchAlerts() {
        val response = RetrofitClient.instance.getAlerts()
        if (response.isSuccessful) {
            _alerts.value = response.body() ?: _alerts.value
        }
    }

    fun fetchLogs(containerId: String? = null) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getLogs(containerId)
                if (response.isSuccessful) {
                    _logs.value = response.body() ?: _logs.value
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun updateStats() {
        val statsResponse = RetrofitClient.instance.getSystemStats()
        if (statsResponse.isSuccessful) {
            _systemStats.value = statsResponse.body() ?: _systemStats.value
        }
    }

    fun toggleContainer(containerId: String, enable: Boolean) {
        // Actualización local inmediata para feedback visual
        _containers.value = _containers.value.map {
            if (it.id == containerId) it.copy(status = if (enable) "running" else "exited") else it
        }

        viewModelScope.launch {
            try {
                val response = if (enable) {
                    RetrofitClient.instance.startContainer(containerId)
                } else {
                    RetrofitClient.instance.stopContainer(containerId)
                }
                
                if (!response.isSuccessful) {
                    // Si falla la API, podrías revertir el estado local o mostrar un error
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
