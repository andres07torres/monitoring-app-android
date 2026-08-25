package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.SystemStatsResponse
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.ui.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<SystemStatsResponse>>(UiState.Loading)
    val uiState: StateFlow<UiState<SystemStatsResponse>> = _uiState.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                fetchStats()
                delay(5000)
            }
        }
    }

    private suspend fun fetchStats() {
        try {
            val response = RetrofitClient.instance.getSystemStats()
            if (response.isSuccessful && response.body() != null) {
                _uiState.value = UiState.Success(response.body()!!)
            } else {
                _uiState.value = UiState.Error("Error al obtener estadísticas: ${response.code()}")
            }
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Error desconocido")
        }
    }
}
