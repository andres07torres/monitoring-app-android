package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.models.SystemStatsResponse
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.ui.UiState
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

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                fetchDashboardData()
                delay(5000)
            }
        }
    }

    private suspend fun fetchDashboardData() {
        try {
            val statsResponse = RetrofitClient.instance.getSystemStats()
            val containersResponse = RetrofitClient.instance.getContainers()

            if (statsResponse.isSuccessful && containersResponse.isSuccessful) {
                val stats = statsResponse.body()
                val containers = containersResponse.body()
                if (stats != null && containers != null) {
                    _uiState.value = UiState.Success(DashboardData(stats, containers))
                }
            } else {
                if (_uiState.value !is UiState.Success) {
                    _uiState.value = UiState.Error("Error al sincronizar con el servidor")
                }
            }
        } catch (e: Exception) {
            if (_uiState.value !is UiState.Success) {
                _uiState.value = UiState.Error(e.message ?: "Error de conexión")
            }
        }
    }
}
