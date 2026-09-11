package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.models.SystemStatsResponse
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.data.network.WebSocketManager
import com.example.inframonitor.data.network.WebSocketState
import com.example.inframonitor.ui.UiState
import com.example.inframonitor.utils.NetworkErrorMapper
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardData(
    val stats: SystemStatsResponse,
    val containers: List<ContainerResponse>
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardData>> = _uiState.asStateFlow()

    private val wsManager = WebSocketManager(viewModelScope)
    val wsState: StateFlow<WebSocketState> = wsManager.connectionState

    private val gson = Gson()

    init {
        startPolling()
        startLiveSystemStream()
    }

    private fun startLiveSystemStream() {
        // Conexión exacta al endpoint WebSocket de métricas en vivo
        // Ruta final: wss://api-infra.andr3sdev7.com/ws/v1/system/stream
        wsManager.connect("ws/v1/system/stream")

        viewModelScope.launch {
            wsManager.messages.collect { message ->
                try {
                    val updatedStats = gson.fromJson(message, SystemStatsResponse::class.java)
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        _uiState.value = UiState.Success(
                            currentState.data.copy(stats = updatedStats)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                fetchDashboardData()
                delay(10000)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetchDashboardData()
            wsManager.connect("ws/v1/system/stream")
        }
    }

    private suspend fun fetchDashboardData() {
        try {
            val statsDeferred = viewModelScope.async { RetrofitClient.instance.getSystemStats() }
            val containersDeferred = viewModelScope.async { RetrofitClient.instance.getContainers() }

            val statsRes = statsDeferred.await()
            val containersRes = containersDeferred.await()

            if (statsRes.isSuccessful && containersRes.isSuccessful) {
                val stats = statsRes.body()
                val containers = containersRes.body()
                
                if (stats != null && containers != null) {
                    _uiState.value = UiState.Success(DashboardData(stats, containers))
                }
            } else {
                if (_uiState.value !is UiState.Success) {
                    val err = if (!statsRes.isSuccessful) NetworkErrorMapper.parseResponseError(statsRes) else NetworkErrorMapper.parseResponseError(containersRes)
                    _uiState.value = UiState.Error(err)
                }
            }
        } catch (e: Exception) {
            if (_uiState.value !is UiState.Success) {
                _uiState.value = UiState.Error(NetworkErrorMapper.parseThrowable(e))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.disconnect()
    }
}
